package com.visionselect.backend.video.dto;

import com.visionselect.backend.video.entity.Video;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Matches the {@code Video} schema in openapi.yaml. {@code
 * durationSeconds} is nullable and, per that schema's own annotation, "not
 * trusted from client" - it only ever comes from {@link Video#getDurationSeconds()},
 * never from a request DTO.
 */
public record VideoResponse(
        UUID id,
        String filename,
        long fileSizeBytes,
        String mimeType,
        String storageKey,
        BigDecimal durationSeconds,
        String status,
        UUID uploadedBy,
        UUID playerId,
        Instant createdAt
) {
    public static VideoResponse from(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getFilename(),
                video.getFileSizeBytes(),
                video.getMimeType(),
                video.getStorageKey(),
                video.getDurationSeconds(),
                video.getStatus().name(),
                video.getUploadedBy(),
                video.getPlayerId(),
                video.getCreatedAt()
        );
    }
}
