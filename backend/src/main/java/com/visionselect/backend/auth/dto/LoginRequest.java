package com.visionselect.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/login request body.
 * Matches frontend {@code LoginRequest}: {@code { email, password }}.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {
}
