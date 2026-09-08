package com.visionselect.backend.auth.entity;

/**
 * Application roles per security-contract.md.
 *
 * <p>Stored as a string in the {@code users.role} column (CHECK constraint
 * in V1 migration) and as a {@code ROLE_<name>} Spring authority in the
 * security context so {@code @PreAuthorize("hasAnyRole('ADMIN','COACH')")}
 * works as written in {@link com.visionselect.backend.api.v1.videos.VideoController}.
 *
 * <p>Tests use {@code UserRole.ADMIN} and {@code UserRole.COACH} directly
 * via {@code authenticateAs(UserRole role)}.
 */
public enum UserRole {
    ADMIN,
    COACH,
    SELECTOR,
    PLAYER
}
