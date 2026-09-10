package com.visionselect.backend.ai.dto;

import com.visionselect.backend.ai.entity.AiJob;
import com.visionselect.backend.ai.entity.AiJobStatus;
import com.visionselect.backend.ai.entity.AiJobType;
import com.visionselect.backend.ai.entity.AiProvider;

import java.time.Instant;
import java.util.UUID;

/** Response body for AI job endpoints. Does not expose internal error details in full. */
public record AiJobResponse(
        UUID id,
        UUID videoId,
        UUID playerId,
        UUID submittedBy,
        AiJobType jobType,
        AiProvider provider,
        AiJobStatus status,
        Short progressPct,
        short attemptCount,
        short maxAttempts,
        String errorCode,
        String idempotencyKey,
        Instant queuedAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AiJobResponse from(AiJob job) {
        return new AiJobResponse(
                job.getId(), job.getVideoId(), job.getPlayerId(), job.getSubmittedBy(),
                job.getJobType(), job.getProvider(), job.getStatus(), job.getProgressPct(),
                job.getAttemptCount(), job.getMaxAttempts(), job.getErrorCode(),
                job.getIdempotencyKey(), job.getQueuedAt(), job.getStartedAt(),
                job.getCompletedAt(), job.getFailedAt(), job.getCreatedAt(), job.getUpdatedAt());
    }
}
