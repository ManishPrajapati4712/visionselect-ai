package com.visionselect.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Progress update payload from Python service. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AiProgressRequest(
        @NotNull @Min(0) @Max(100)
        Short progressPct,

        String stage
) { }
