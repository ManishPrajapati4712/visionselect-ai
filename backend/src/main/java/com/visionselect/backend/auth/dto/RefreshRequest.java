package com.visionselect.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/refresh request body.
 * Matches frontend: {@code { refreshToken }}.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record RefreshRequest(

        @NotBlank
        String refreshToken
) {
}
