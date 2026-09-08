package com.visionselect.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/register request body.
 *
 * <p>Matches the frontend {@code RegisterRequest} type:
 * {@code { email, password, displayName, role }}.
 *
 * <p>Role is constrained to the three self-registration values; ADMIN
 * accounts are not creatable via the public registration endpoint.
 * {@code @JsonIgnoreProperties(ignoreUnknown = false)} rejects any extra
 * fields.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record RegisterRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @NotBlank
        @Size(min = 2, max = 100)
        String displayName,

        @NotBlank
        @Pattern(regexp = "COACH|SELECTOR|PLAYER",
                message = "role must be one of: COACH, SELECTOR, PLAYER")
        String role
) {
}
