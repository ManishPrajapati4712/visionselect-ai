package com.visionselect.backend.ai.service;

import com.visionselect.backend.ai.dto.AiCallbackRequest;
import com.visionselect.backend.ai.dto.AiJobCreateRequest;
import com.visionselect.backend.ai.dto.AiJobResponse;
import com.visionselect.backend.ai.entity.AiJob;
import com.visionselect.backend.ai.entity.AiJobStatus;
import com.visionselect.backend.ai.entity.AiJobType;
import com.visionselect.backend.ai.entity.AiProvider;
import com.visionselect.backend.ai.provider.AiProviderClient;
import com.visionselect.backend.ai.repository.AiAnalysisResultRepository;
import com.visionselect.backend.ai.repository.AiJobRepository;
import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.player.entity.Player;
import com.visionselect.backend.player.repository.PlayerRepository;
import com.visionselect.backend.storage.StorageProvider;
import com.visionselect.backend.video.entity.Video;
import com.visionselect.backend.video.entity.VideoStatus;
import com.visionselect.backend.video.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AiJobService}.
 *
 * <p>No Spring context. All dependencies mocked. Tests cover:
 * job submission, idempotency, RBAC, deduplication, cancellation,
 * state transitions, callback handling, retry logic.
 */
class AiJobServiceTest {

    private AiJobRepository jobRepository;
    private AiAnalysisResultRepository resultRepository;
    private VideoRepository videoRepository;
    private PlayerRepository playerRepository;
    private StorageProvider storageProvider;
    private AiProviderClient providerClient;
    private AiJobService service;

    private static final UUID ADMIN_ID  = UUID.randomUUID();
    private static final UUID COACH_ID  = UUID.randomUUID();
    private static final UUID VIDEO_ID  = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jobRepository    = mock(AiJobRepository.class);
        resultRepository = mock(AiAnalysisResultRepository.class);
        videoRepository  = mock(VideoRepository.class);
        playerRepository = mock(PlayerRepository.class);
        storageProvider  = mock(StorageProvider.class);
        providerClient   = mock(AiProviderClient.class);

        service = new AiJobService(jobRepository, resultRepository, videoRepository,
                playerRepository, storageProvider, providerClient,
                "http://localhost:8080", 60L);

        // Default stubs
        Video video = new Video("test.mp4", "videos/test/uuid.mp4", "video/mp4",
                10_000_000L, VideoStatus.UPLOADED, COACH_ID, PLAYER_ID);
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(video));

        Player player = mock(Player.class);
        when(player.getDeletedAt()).thenReturn(null);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));

        when(storageProvider.issueDownloadUrl(any(), any(Duration.class)))
                .thenReturn("http://localhost:8080/local-storage/download/token");
        when(jobRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(jobRepository.findActiveJobForVideoAndType(any(), any())).thenReturn(Optional.empty());
        when(jobRepository.save(any(AiJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resultRepository.findByAiJobId(any())).thenReturn(Optional.empty());
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(ADMIN_ID, UserRole.ADMIN, UUID.randomUUID());
    }

    private AuthenticatedUser coach() {
        return new AuthenticatedUser(COACH_ID, UserRole.COACH, UUID.randomUUID());
    }

    private AiJobCreateRequest createRequest() {
        return new AiJobCreateRequest(VIDEO_ID, PLAYER_ID, "FULL_ANALYSIS", UUID.randomUUID().toString());
    }

    // ── Submit tests ──────────────────────────────────────────────────────────

    @Test
    void submitJob_asAdmin_succeeds() {
        AiJobResponse response = service.submit(admin(), createRequest());
        verify(jobRepository).save(any(AiJob.class));
        assertThat(response.jobType()).isEqualTo(AiJobType.FULL_ANALYSIS);
        assertThat(response.status()).isEqualTo(AiJobStatus.QUEUED);
    }

    @Test
    void submitJob_asCoach_ownVideo_succeeds() {
        // COACH_ID matches video.uploadedBy — should succeed
        AiJobResponse response = service.submit(coach(), createRequest());
        assertThat(response.status()).isEqualTo(AiJobStatus.QUEUED);
    }

    @Test
    void submitJob_asCoach_anotherCoachsVideo_throws403() {
        // Video uploaded by a different user
        Video foreignVideo = new Video("test.mp4", "videos/other/uuid.mp4", "video/mp4",
                1_000L, VideoStatus.UPLOADED, UUID.randomUUID(), PLAYER_ID);
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(foreignVideo));

        assertThatThrownBy(() -> service.submit(coach(), createRequest()))
                .isInstanceOf(AiJobService.AiJobForbiddenException.class)
                .hasMessageContaining("COACH may only submit");
    }

    @Test
    void submitJob_videoNotFound_throws404() {
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(admin(), createRequest()))
                .isInstanceOf(AiJobService.VideoNotFoundException.class);
    }

    @Test
    void submitJob_playerNotFound_throws404() {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(admin(), createRequest()))
                .isInstanceOf(AiJobService.PlayerNotFoundException.class);
    }

    @Test
    void submitJob_idempotencyKey_returnsExistingJob() {
        AiJob existing = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "same-key", (short) 3);
        when(jobRepository.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        AiJobCreateRequest request = new AiJobCreateRequest(VIDEO_ID, PLAYER_ID, "FULL_ANALYSIS", "same-key");
        AiJobResponse response = service.submit(admin(), request);

        // Must NOT create a new job
        verify(jobRepository, never()).save(any());
        assertThat(response.idempotencyKey()).isEqualTo("same-key");
    }

    @Test
    void submitJob_duplicateActiveJob_throws409() {
        AiJob active = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "other-key", (short) 3);
        when(jobRepository.findActiveJobForVideoAndType(eq(VIDEO_ID), eq(AiJobType.FULL_ANALYSIS)))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.submit(admin(), createRequest()))
                .isInstanceOf(AiJobService.DuplicateActiveJobException.class)
                .hasMessageContaining("active job already exists");
    }

    // ── Cancel tests ──────────────────────────────────────────────────────────

    @Test
    void cancelJob_asAdmin_anyJob_succeeds() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, COACH_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "k1", (short) 3);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AiJobResponse response = service.cancel(jobId, admin());
        assertThat(response.status()).isEqualTo(AiJobStatus.CANCELLED);
    }

    @Test
    void cancelJob_asCoach_ownJob_succeeds() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, COACH_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "k2", (short) 3);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AiJobResponse response = service.cancel(jobId, coach());
        assertThat(response.status()).isEqualTo(AiJobStatus.CANCELLED);
    }

    @Test
    void cancelJob_asCoach_anotherCoachsJob_throws403() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, UUID.randomUUID(), // different user
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "k3", (short) 3);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.cancel(jobId, coach()))
                .isInstanceOf(AiJobService.AiJobForbiddenException.class);
    }

    @Test
    void cancelJob_processingJob_throws409() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "k4", (short) 3);
        job.markProcessing();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.cancel(jobId, admin()))
                .isInstanceOf(AiJobService.InvalidJobStateException.class)
                .hasMessageContaining("QUEUED jobs can be cancelled");
    }

    // ── State machine tests ───────────────────────────────────────────────────

    @Test
    void stateTransition_QUEUED_to_PROCESSING() {
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "sm1", (short) 3);
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.QUEUED);
        job.markProcessing();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.PROCESSING);
    }

    @Test
    void stateTransition_PROCESSING_to_COMPLETED() {
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "sm2", (short) 3);
        job.markProcessing();
        job.markCompleted();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.COMPLETED);
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void stateTransition_PROCESSING_to_FAILED() {
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "sm3", (short) 3);
        job.markProcessing();
        job.markFailed("TEST_ERROR", "test failure");
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(job.getAttemptCount()).isEqualTo((short) 1);
    }

    @Test
    void stateTransition_FAILED_to_RETRYING() {
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "sm4", (short) 3);
        job.markProcessing();
        job.markFailed("ERR", "err");
        job.scheduleRetry(java.time.Instant.now().plusSeconds(30));
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.RETRYING);
        assertThat(job.getNextRetryAt()).isNotNull();
    }

    @Test
    void stateTransition_RETRYING_to_PROCESSING() {
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "sm5", (short) 3);
        job.markProcessing();
        job.markFailed("ERR", "err");
        job.scheduleRetry(java.time.Instant.now().plusSeconds(30));
        job.markProcessing(); // RETRYING -> PROCESSING is valid
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.PROCESSING);
    }

    @Test
    void stateTransition_invalid_COMPLETED_to_anything_throws() {
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "sm6", (short) 3);
        job.markProcessing();
        job.markCompleted();
        assertThatThrownBy(job::markProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid AI job state transition");
    }

    // ── Callback tests ────────────────────────────────────────────────────────

    @Test
    void callback_completed_persistsResult() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "cb1", (short) 3);
        job.markProcessing();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AiCallbackRequest.ResultPayload result = new AiCallbackRequest.ResultPayload(
                BigDecimal.valueOf(82.5), BigDecimal.valueOf(88.0), null,
                BigDecimal.valueOf(76.0), null, BigDecimal.valueOf(0.91),
                Map.of(), Map.of("overall", "Strong technique"), Map.of(),
                Map.of("provider", "GEMINI", "model", "gemini-1.5-pro"));
        AiCallbackRequest callback = new AiCallbackRequest("COMPLETED", result, null);

        service.handleCallback(jobId, callback);

        verify(resultRepository).save(any());
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.COMPLETED);
    }

    @Test
    void callback_failed_withRetriesRemaining_schedulesRetry() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "cb2", (short) 3);
        job.markProcessing();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AiCallbackRequest.ErrorPayload error = new AiCallbackRequest.ErrorPayload("TIMEOUT", "timed out");
        AiCallbackRequest callback = new AiCallbackRequest("FAILED", null, error);

        service.handleCallback(jobId, callback);

        assertThat(job.getStatus()).isEqualTo(AiJobStatus.RETRYING);
        assertThat(job.getNextRetryAt()).isNotNull();
    }

    @Test
    void callback_failed_maxAttemptsReached_terminalFailed() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "cb3", (short) 1); // max=1
        job.markProcessing();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AiCallbackRequest callback = new AiCallbackRequest("FAILED",
                null, new AiCallbackRequest.ErrorPayload("ERR", "err"));

        service.handleCallback(jobId, callback);

        // attempt_count=1 now == max_attempts=1, no retries remain
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(job.hasRetriesRemaining()).isFalse();
    }

    @Test
    void duplicateCallback_isIgnored() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "cb4", (short) 3);
        job.markProcessing();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        // Simulate result already persisted
        when(resultRepository.findByAiJobId(jobId))
                .thenReturn(Optional.of(mock(com.visionselect.backend.ai.entity.AiAnalysisResult.class)));

        AiCallbackRequest callback = new AiCallbackRequest("COMPLETED",
                new AiCallbackRequest.ResultPayload(
                        BigDecimal.valueOf(80), null, null, null, null, null,
                        null, null, null, Map.of()),
                null);

        service.handleCallback(jobId, callback);

        // Must not save another result
        verify(resultRepository, never()).save(any());
    }

    @Test
    void callback_jobNotProcessing_throws409() {
        UUID jobId = UUID.randomUUID();
        AiJob job = new AiJob(VIDEO_ID, PLAYER_ID, ADMIN_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE, "cb5", (short) 3);
        // Job is still QUEUED
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AiCallbackRequest callback = new AiCallbackRequest("COMPLETED",
                new AiCallbackRequest.ResultPayload(
                        BigDecimal.valueOf(80), null, null, null, null, null,
                        null, null, null, Map.of()),
                null);

        assertThatThrownBy(() -> service.handleCallback(jobId, callback))
                .isInstanceOf(AiJobService.InvalidJobStateException.class);
    }
}
