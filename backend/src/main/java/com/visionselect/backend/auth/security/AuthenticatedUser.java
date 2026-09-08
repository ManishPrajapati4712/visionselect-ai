package com.visionselect.backend.auth.security;

import com.visionselect.backend.auth.entity.UserRole;

import java.util.UUID;

/**
 * The object stored in the Spring SecurityContext as the authenticated
 * principal for JWT-authenticated requests.
 *
 * <p>Constructor signature is exactly:
 * {@code new AuthenticatedUser(UUID userId, UserRole role, UUID sessionId)}
 * as used in {@link com.visionselect.backend.api.v1.videos.VideoControllerTest}:
 * <pre>
 *   AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), role, UUID.randomUUID());
 * </pre>
 *
 * <p>{@code sessionId} is the JWT's {@code jti} claim, included so a
 * per-session revocation mechanism can be added later without changing
 * the constructor signature.
 */
public record AuthenticatedUser(
        UUID userId,
        UserRole role,
        UUID sessionId
) {
}
