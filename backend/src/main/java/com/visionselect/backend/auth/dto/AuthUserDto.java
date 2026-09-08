package com.visionselect.backend.auth.dto;

import com.visionselect.backend.auth.entity.User;

import java.util.UUID;

/**
 * Authenticated user details embedded in auth responses.
 * Matches frontend {@code AuthUser}: {@code { id, email, displayName, role }}.
 *
 * <p>The JSON field is {@code displayName} for frontend API compatibility.
 * The underlying DB column is {@code full_name}; the Java getter is
 * {@link com.visionselect.backend.auth.entity.User#getFullName()}.
 */
public record AuthUserDto(
        UUID id,
        String email,
        String displayName,
        String role
) {
    public static AuthUserDto from(User user) {
        return new AuthUserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),      // DB column: full_name
                user.getRole().name()
        );
    }
}
