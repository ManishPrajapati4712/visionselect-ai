package com.visionselect.backend.ai.repository;

import com.visionselect.backend.ai.entity.AiJob;
import com.visionselect.backend.ai.entity.AiJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

    /** Idempotency lookup — find job by the client-supplied idempotency key. */
    Optional<AiJob> findByIdempotencyKey(String idempotencyKey);

    /** Find all jobs for a player, newest first. */
    List<AiJob> findByPlayerIdOrderByQueuedAtDesc(UUID playerId);

    /** Find all jobs for a video, newest first. */
    List<AiJob> findByVideoIdOrderByQueuedAtDesc(UUID videoId);

    /** Find all jobs submitted by a specific user. */
    List<AiJob> findBySubmittedByOrderByQueuedAtDesc(UUID submittedBy);

    /** Find QUEUED and RETRYING jobs ready to be dispatched, ordered by queued_at. */
    @Query("SELECT j FROM AiJob j WHERE j.status IN (:statuses) ORDER BY j.queuedAt ASC")
    List<AiJob> findDispatchableJobs(
            @Param("statuses") List<AiJobStatus> statuses);

    /**
     * Find RETRYING jobs whose next_retry_at has elapsed and are ready to be re-dispatched.
     */
    @Query("SELECT j FROM AiJob j WHERE j.status = 'RETRYING' AND j.nextRetryAt <= :now")
    List<AiJob> findRetryableJobs(@Param("now") Instant now);

    /**
     * Find PROCESSING jobs that started more than {@code stallThreshold} ago
     * and have not received a callback — these may be stalled.
     */
    @Query("SELECT j FROM AiJob j WHERE j.status = 'PROCESSING' AND j.startedAt <= :stallThreshold")
    List<AiJob> findStalledProcessingJobs(@Param("stallThreshold") Instant stallThreshold);

    /**
     * Check for an existing active job for the given video and job type.
     * Used to enforce the partial unique index constraint at the application level
     * before attempting a DB insert (gives a cleaner error than a constraint violation).
     */
    @Query("SELECT j FROM AiJob j WHERE j.videoId = :videoId AND j.jobType = :jobType " +
           "AND j.status IN ('QUEUED','PROCESSING','RETRYING')")
    Optional<AiJob> findActiveJobForVideoAndType(
            @Param("videoId") UUID videoId,
            @Param("jobType") com.visionselect.backend.ai.entity.AiJobType jobType);
}
