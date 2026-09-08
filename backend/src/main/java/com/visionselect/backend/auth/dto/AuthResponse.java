package com.visionselect.backend.auth.dto;

/**
 * Auth response returned by register, login, and refresh endpoints.
 * Matches the frontend {@code AuthResponse} type:
 * {@code { accessToken, refreshToken, user }}.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        AuthUserDto user
) {
}
