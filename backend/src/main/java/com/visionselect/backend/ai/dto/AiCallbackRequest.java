package com.visionselect.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Callback payload sent by the Python AI service to
 * {@code POST /internal/v1/ai/jobs/{id}/callback}.
 *
 * <p>Authentication is via {@code X-Internal-Service-Token} header, not JWT.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AiCallbackRequest(

        @NotNull(message = "status is required")
        @Pattern(regexp = "COMPLETED|FAILED", message = "status must be COMPLETED or FAILED")
        String status,

        /** Present when status=COMPLETED. */
        @Valid
        ResultPayload result,

        /** Present when status=FAILED. */
        @Valid
        ErrorPayload error
) {
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record ResultPayload(
            BigDecimal overallScore,
            BigDecimal battingScore,
            BigDecimal bowlingScore,
            BigDecimal fieldingScore,
            BigDecimal fitnessScore,
            BigDecimal confidence,
            Map<String, Object> featureContributions,
            Map<String, Object> explanationText,
            Map<String, Object> rawMetrics,
            @NotNull Map<String, Object> modelMetadata
    ) { }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record ErrorPayload(
            @NotBlank String code,
            @NotBlank String message
    ) { }
}
