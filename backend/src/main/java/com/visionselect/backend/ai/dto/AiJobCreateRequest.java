package com.visionselect.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/ai/jobs}.
 *
 * <p>The client must supply a unique {@code idempotencyKey} (e.g. a UUID they generate)
 * to prevent duplicate submissions on retry. The same key submitted a second time
 * returns the existing job rather than creating a new one.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AiJobCreateRequest(

        @NotNull(message = "videoId is required")
        UUID videoId,

        @NotNull(message = "playerId is required")
        UUID playerId,

        @NotNull(message = "jobType is required")
        @Pattern(regexp = "FULL_ANALYSIS|BATTING_ONLY|BOWLING_ONLY|FIELDING_ONLY",
                 message = "jobType must be one of: FULL_ANALYSIS, BATTING_ONLY, BOWLING_ONLY, FIELDING_ONLY")
        String jobType,

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey
) { }
