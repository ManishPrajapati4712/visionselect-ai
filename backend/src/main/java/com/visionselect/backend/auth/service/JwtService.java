package com.visionselect.backend.auth.service;

import com.visionselect.backend.auth.config.JwtProperties;
import com.visionselect.backend.auth.entity.User;
import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT access-token lifecycle: generation and validation.
 *
 * <p>Access token claims:
 * <ul>
 *   <li>{@code sub}  — user ID (UUID string)</li>
 *   <li>{@code role} — {@link UserRole} name</li>
 *   <li>{@code jti}  — session UUID (for future per-session revocation)</li>
 *   <li>{@code iat}  — issued-at</li>
 *   <li>{@code exp}  — expiry</li>
 * </ul>
 *
 * <p>The signing key is derived from the configured secret using HMAC-SHA256
 * (HS256). The secret must be at least 256 bits (32 characters) — enforced
 * at key-derivation time by jjwt.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long accessTokenExpirationMinutes;

    public JwtService(JwtProperties jwtProperties) {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMinutes = jwtProperties.accessTokenExpirationMinutes();
    }

    /**
     * Generate a signed access token for the given user.
     *
     * @param user      the authenticated user
     * @param sessionId the JWT jti — unique per login/refresh
     * @return a signed JWT string
     */
    public String generateAccessToken(User user, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .id(sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate a token and extract the principal, or return empty if invalid.
     */
    public Optional<AuthenticatedUser> validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            UUID sessionId = UUID.fromString(claims.getId());

            return Optional.of(new AuthenticatedUser(userId, role, sessionId));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
