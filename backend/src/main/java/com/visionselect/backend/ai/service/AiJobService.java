package com.visionselect.backend.ai.service;

import com.visionselect.backend.ai.dto.AiCallbackRequest;
import com.visionselect.backend.ai.dto.AiJobCreateRequest;
import com.visionselect.backend.ai.dto.AiJobResponse;
import com.visionselect.backend.ai.entity.AiAnalysisResult;
import com.visionselect.backend.ai.entity.AiJob;
import com.visionselect.backend.ai.entity.AiJobStatus;
import com.visionselect.backend.ai.entity.AiJobType;
import com.visionselect.backend.ai.entity.AiProvider;
import com.visionselect.backend.ai.provider.AiProviderClient;
import com.visionselect.backend.ai.repository.AiAnalysisResultRepository;
import com.visionselect.backend.ai.repository.AiJobRepository;
import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.exception.ApiException;
import com.visionselect.backend.player.repository.PlayerRepository;
import com.visionselect.backend.storage.StorageProvider;
import com.visionselect.backend.video.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for the V4 AI Job Pipeline.
 *
 * <p>Orchestration responsibilities:
 * <ul>
 *   <li>Validate video/player ownership and existence before creating a job.</li>
 *   <li>Enforce idempotency: return existing job if idempotencyKey already seen.</li>
 *   <li>Prevent duplicate active jobs via pre-check (mirrors partial unique index).</li>
 *   <li>Persist the job in QUEUED state, then dispatch asynchronously.</li>
 *   <li>Maintain state-machine transitions (see {@link AiJobStatus}).</li>
 *   <li>Handle Python provider failures with exponential-backoff retry.</li>
 *   <li>Persist {@link AiAnalysisResult} on successful callback.</li>
 *   <li>Update {@code videos.status} on completion.</li>
 * </ul>
 *
 * <p>RBAC is enforced at the controller level via {@code @PreAuthorize}.
 * Ownership checks (COACH can only submit for own videos) are enforced here.
 *
 * <p>Transactions are kept SHORT. The long-running HTTP call to Python runs
 * OUTSIDE any transaction to avoid holding a DB connection for up to 30 seconds.
 */
@Service
public class AiJobService {

    private static final Logger log = LoggerFactory.getLogger(AiJobService.class);

    private final AiJobRepository jobRepository;
    private final AiAnalysisResultRepository resultRepository;
    private final VideoRepository videoRepository;
    private final PlayerRepository playerRepository;
    private final StorageProvider storageProvider;
    private final AiProviderClient providerClient;

    private final String internalBaseUrl;
    private final long downloadUrlTtlMinutes;

    // Retry backoff: attempt 1 -> 30s, attempt 2 -> 60s, attempt 3 -> 120s
    private static final long[] RETRY_BACKOFF_SECONDS = {30L, 60L, 120L};
    // Jobs stuck in PROCESSING for more than 10 minutes are considered stalled
    private static final Duration STALL_THRESHOLD = Duration.ofMinutes(10);

    public AiJobService(AiJobRepository jobRepository,
                        AiAnalysisResultRepository resultRepository,
                        VideoRepository videoRepository,
                        PlayerRepository playerRepository,
                        StorageProvider storageProvider,
                        AiProviderClient providerClient,
                        @Value("${app.ai.internal-base-url:http://localhost:8080}") String internalBaseUrl,
                        @Value("${app.ai.download-url-ttl-minutes:60}") long downloadUrlTtlMinutes) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
        this.videoRepository = videoRepository;
        this.playerRepository = playerRepository;
        this.storageProvider = storageProvider;
        this.providerClient = providerClient;
        this.internalBaseUrl = internalBaseUrl.replaceAll("/+$", "");
        this.downloadUrlTtlMinutes = downloadUrlTtlMinutes;
    }

    // -- Submit ---------------------------------------------------------------

    /**
     * Submits a new AI analysis job.
     *
     * <p>Idempotency: if {@code request.idempotencyKey()} matches an existing job,
     * returns that job without creating a duplicate.
     *
     * <p>COACH ownership: a COACH may only submit jobs for videos they uploaded.
     */
    @Transactional
    public AiJobResponse submit(AuthenticatedUser principal, AiJobCreateRequest request) {
        // 1. Idempotency check — return existing job if key already used
        Optional<AiJob> existing = jobRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotency hit: returning existing job {} for key={}",
                    existing.get().getId(), request.idempotencyKey());
            return AiJobResponse.from(existing.get());
        }

        // 2. Validate video exists and is not soft-deleted
        var video = videoRepository.findById(request.videoId())
                .orElseThrow(() -> new VideoNotFoundException("Video not found: " + request.videoId()));

        // 3. Validate player exists and is active
        var player = playerRepository.findById(request.playerId())
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found: " + request.playerId()));

        // 4. COACH may only submit for their own videos
        if (principal.role() == UserRole.COACH &&
                !video.getUploadedBy().equals(principal.userId())) {
            throw new AiJobForbiddenException(
                    "COACH may only submit analysis jobs for videos they uploaded");
        }

        // 5. Deduplication: block if an active job exists for this video+type
        AiJobType jobType = AiJobType.valueOf(request.jobType());
        jobRepository.findActiveJobForVideoAndType(request.videoId(), jobType).ifPresent(j -> {
            throw new DuplicateActiveJobException(
                    "An active job already exists for this video and job type: " + j.getId());
        });

        // 6. Create and persist the job in QUEUED state
        AiJob job = new AiJob(
                request.videoId(),
                request.playerId(),
                principal.userId(),
                jobType,
                AiProvider.PYTHON_AI_SERVICE,
                request.idempotencyKey(),
                (short) 3
        );
        job = jobRepository.save(job);
        log.info("AI job created jobId={} videoId={} playerId={} jobType={}",
                job.getId(), job.getVideoId(), job.getPlayerId(), job.getJobType());

        // 7. Dispatch asynchronously (OUTSIDE the current transaction)
        dispatchJob(job.getId());
        return AiJobResponse.from(job);
    }

    // -- Async dispatch -------------------------------------------------------

    /**
     * Picks up a QUEUED job and calls the AI provider.
     * Runs on the {@code aiJobExecutor} thread pool, NOT in a transaction while doing HTTP.
     */
    @Async("aiJobExecutor")
    public void dispatchJob(UUID jobId) {
        // Short transaction to fetch and transition to PROCESSING
        AiJob job = markProcessingInTransaction(jobId);
        if (job == null) return; // already transitioned by another thread

        // Generate signed video URL (short-lived; never persisted)
        String signedUrl = getSignedVideoUrl(job.getVideoId());

        String callbackUrl = internalBaseUrl + "/internal/v1/ai/jobs/" + job.getId() + "/callback";
        String progressUrl = internalBaseUrl + "/internal/v1/ai/jobs/" + job.getId() + "/progress";

        try {
            providerClient.submitJob(
                    job.getId(), job.getVideoId(), job.getPlayerId(),
                    signedUrl, job.getJobType().name(),
                    callbackUrl, progressUrl,
                    job.getAttemptCount() + 1);
            log.info("AI job dispatched to provider jobId={}", job.getId());
        } catch (Exception e) {
            log.error("Failed to dispatch AI job jobId={}: {}", job.getId(), e.getMessage());
            handleProviderFailure(job.getId(), "DISPATCH_FAILED", e.getMessage());
        }
    }

    @Transactional
    protected AiJob markProcessingInTransaction(UUID jobId) {
        AiJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return null;
        // Accept both QUEUED (initial dispatch) and RETRYING (retry dispatch)
        if (job.getStatus() != AiJobStatus.QUEUED && job.getStatus() != AiJobStatus.RETRYING) {
            log.warn("Job {} is not in a dispatchable state: {}", jobId, job.getStatus());
            return null;
        }
        job.markProcessing();
        return jobRepository.save(job);
    }

    private String getSignedVideoUrl(UUID videoId) {
        var video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("Video not found: " + videoId));
        return storageProvider.issueDownloadUrl(
                video.getStorageKey(), Duration.ofMinutes(downloadUrlTtlMinutes));
    }

    // -- Callback handling ----------------------------------------------------

    /**
     * Handles the callback from the Python provider.
     * Validates state, persists result or failure, schedules retry if applicable.
     * Must be called from the internal callback controller.
     */
    @Transactional
    public void handleCallback(UUID jobId, AiCallbackRequest callback) {
        AiJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AiJobNotFoundException("Job not found: " + jobId));

        if (job.getStatus() != AiJobStatus.PROCESSING) {
            throw new InvalidJobStateException(
                    "Cannot receive callback for job in state: " + job.getStatus());
        }

        // Idempotency: if result already persisted, return without duplicate
        if (resultRepository.findByAiJobId(jobId).isPresent()) {
            log.warn("Duplicate callback received for jobId={} — ignoring", jobId);
            return;
        }

        if ("COMPLETED".equals(callback.status())) {
            handleSuccess(job, callback.result());
        } else {
            handleFailure(job, callback.error());
        }
    }

    private void handleSuccess(AiJob job, AiCallbackRequest.ResultPayload result) {
        job.markCompleted();
        jobRepository.save(job);

        AiAnalysisResult analysisResult = new AiAnalysisResult(
                job.getId(), job.getVideoId(), job.getPlayerId(),
                result.overallScore(), result.battingScore(),
                result.bowlingScore(), result.fieldingScore(),
                result.fitnessScore(), result.confidence(),
                result.rawMetrics(), result.featureContributions(),
                result.explanationText(), result.modelMetadata());
        resultRepository.save(analysisResult);

        log.info("AI job completed successfully jobId={} overallScore={}",
                job.getId(), result.overallScore());
    }

    private void handleFailure(AiJob job, AiCallbackRequest.ErrorPayload error) {
        String code = error != null ? error.code() : "UNKNOWN_ERROR";
        String message = error != null ? error.message() : "No error details provided";
        job.markFailed(code, message);

        if (job.hasRetriesRemaining()) {
            long backoffSeconds = retryBackoffSeconds(job.getAttemptCount());
            job.scheduleRetry(Instant.now().plusSeconds(backoffSeconds));
            log.info("AI job failed, scheduling retry #{} in {}s jobId={}",
                    job.getAttemptCount(), backoffSeconds, job.getId());
        } else {
            log.warn("AI job exhausted all retries, marking FAILED terminal jobId={}", job.getId());
        }
        jobRepository.save(job);
    }

    @Transactional
    public void handleProviderFailure(UUID jobId, String errorCode, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() == AiJobStatus.PROCESSING) {
                handleFailure(job, new AiCallbackRequest.ErrorPayload(errorCode, errorMessage));
            }
        });
    }

    // -- Progress update ------------------------------------------------------

    @Transactional
    public void updateProgress(UUID jobId, short progressPct) {
        AiJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AiJobNotFoundException("Job not found: " + jobId));
        if (job.getStatus() == AiJobStatus.PROCESSING) {
            job.updateProgress(progressPct);
            jobRepository.save(job);
        }
    }

    // -- Cancel ---------------------------------------------------------------

    /**
     * Cancels a QUEUED job. PROCESSING jobs cannot be cancelled.
     * ADMIN may cancel any job; COACH may cancel only their own.
     */
    @Transactional
    public AiJobResponse cancel(UUID jobId, AuthenticatedUser principal) {
        AiJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AiJobNotFoundException("Job not found: " + jobId));

        if (principal.role() == UserRole.COACH &&
                !job.getSubmittedBy().equals(principal.userId())) {
            throw new AiJobForbiddenException("COACH may only cancel their own jobs");
        }

        if (job.getStatus() != AiJobStatus.QUEUED) {
            throw new InvalidJobStateException(
                    "Only QUEUED jobs can be cancelled. Current status: " + job.getStatus());
        }

        job.markCancelled();
        return AiJobResponse.from(jobRepository.save(job));
    }

    // -- Queries --------------------------------------------------------------

    @Transactional(readOnly = true)
    public AiJobResponse getById(UUID jobId, AuthenticatedUser principal) {
        AiJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AiJobNotFoundException("Job not found: " + jobId));
        // COACH sees own; ADMIN/SELECTOR see all
        if (principal.role() == UserRole.COACH &&
                !job.getSubmittedBy().equals(principal.userId())) {
            throw new AiJobForbiddenException("Access denied");
        }
        return AiJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<AiJobResponse> listByPlayer(UUID playerId) {
        return jobRepository.findByPlayerIdOrderByQueuedAtDesc(playerId)
                .stream().map(AiJobResponse::from).toList();
    }

    // -- Scheduled retry poller -----------------------------------------------

    /** Polls every 30 seconds for RETRYING jobs whose next_retry_at has elapsed. */
    @Scheduled(fixedDelay = 30_000)
    public void retryPoller() {
        List<AiJob> retryable = jobRepository.findRetryableJobs(Instant.now());
        for (AiJob job : retryable) {
            log.info("Retrying job jobId={} attempt={}", job.getId(), job.getAttemptCount() + 1);
            dispatchJob(job.getId());
        }
    }

    /** Polls every 60 seconds for PROCESSING jobs stuck longer than STALL_THRESHOLD. */
    @Scheduled(fixedDelay = 60_000)
    public void stalledJobPoller() {
        Instant threshold = Instant.now().minus(STALL_THRESHOLD);
        List<AiJob> stalled = jobRepository.findStalledProcessingJobs(threshold);
        for (AiJob job : stalled) {
            log.warn("Stalled job detected jobId={} startedAt={}", job.getId(), job.getStartedAt());
            handleProviderFailure(job.getId(), "JOB_TIMEOUT",
                    "Job exceeded maximum processing time of " + STALL_THRESHOLD.toMinutes() + " minutes");
        }
    }

    // -- Helpers --------------------------------------------------------------

    private long retryBackoffSeconds(int attemptCount) {
        int idx = Math.min(attemptCount, RETRY_BACKOFF_SECONDS.length - 1);
        return RETRY_BACKOFF_SECONDS[idx];
    }

    // -- Inner exception classes (same pattern as PlayerService) --------------

    public static class VideoNotFoundException extends ApiException {
        public VideoNotFoundException(String message) {
            super(HttpStatus.NOT_FOUND, "AI_JOB_VIDEO_NOT_FOUND", message);
        }
    }

    public static class PlayerNotFoundException extends ApiException {
        public PlayerNotFoundException(String message) {
            super(HttpStatus.NOT_FOUND, "AI_JOB_PLAYER_NOT_FOUND", message);
        }
    }

    public static class AiJobNotFoundException extends ApiException {
        public AiJobNotFoundException(String message) {
            super(HttpStatus.NOT_FOUND, "AI_JOB_NOT_FOUND", message);
        }
    }

    public static class AiJobForbiddenException extends ApiException {
        public AiJobForbiddenException(String message) {
            super(HttpStatus.FORBIDDEN, "AI_JOB_FORBIDDEN", message);
        }
    }

    public static class DuplicateActiveJobException extends ApiException {
        public DuplicateActiveJobException(String message) {
            super(HttpStatus.CONFLICT, "AI_JOB_ALREADY_ACTIVE", message);
        }
    }

    public static class InvalidJobStateException extends ApiException {
        public InvalidJobStateException(String message) {
            super(HttpStatus.CONFLICT, "AI_JOB_INVALID_STATE", message);
        }
    }
}
