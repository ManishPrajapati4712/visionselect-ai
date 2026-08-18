package com.visionselect.backend.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Step 2 request (register metadata after a successful direct upload).
 * Matches {@code VideoCreateRequest} in openapi.yaml exactly: {@code
 * storageKey}, {@code filename}, {@code mimeType}, {@code fileSizeBytes},
 * optional {@code playerId}.
 *
 * <p>Deliberately no {@code status} field, for the same reason {@code
 * RegisterRequest} has no {@code role} field: {@link
 * com.visionselect.backend.video.entity.VideoStatus} is assigned by
 * {@link com.visionselect.backend.video.service.VideoService} based on
 * the storage existence check this call triggers, never by client input.
 * {@code @JsonIgnoreProperties(ignoreUnknown = false)} makes that explicit
 * at the DTO level, same reasoning as {@code RegisterRequest}.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record VideoCreateRequest(

        @NotBlank
        String storageKey,

        @NotBlank
        String filename,

        @NotBlank
        String mimeType,

        @NotNull
        @Positive
        @Max(524_288_000L)
        Long fileSizeBytes,

        UUID playerId
) {
}
