package com.visionselect.backend.video.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Step 1 request. Matches {@code UploadUrlRequest} in openapi.yaml exactly:
 * {@code filename}, {@code mimeType}, {@code fileSizeBytes}, optional
 * {@code playerId}.
 *
 * <p>{@code mimeType} is validated against the exact two allowed values
 * ({@code video/mp4}, {@code video/quicktime}) in {@link
 * com.visionselect.backend.video.service.VideoService}, not here - the
 * failure needs to map to {@code 415}, not the generic {@code 400}
 * {@code @Valid} produces, so it can't be a bean-validation annotation on
 * this field (there's no {@code @UnsupportedMediaType} constraint; a
 * pattern/enum check here would surface as a 400).
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = false)}, same reasoning
 * as {@code RegisterRequest}: makes rejection of unrecognized fields an
 * explicit, self-documenting property of this DTO. Most pointedly, this
 * closes off a client trying to smuggle a client-chosen {@code storageKey}
 * or {@code status} in at this step - both are backend-generated /
 * backend-owned per storage-contract.md and data-dictionary.md respectively.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UploadUrlRequest(

        @NotBlank
        String filename,

        @NotBlank
        String mimeType,

        @NotNull
        @Positive
        @Max(524_288_000L) // 500 MB cap, per openapi.yaml
        Long fileSizeBytes,

        UUID playerId
) {
}
