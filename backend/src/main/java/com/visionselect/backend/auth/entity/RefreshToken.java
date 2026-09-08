package com.visionselect.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the {@code refresh_tokens} table from V1 migration.
 *
 * <p>Column layout matches the live database (visionselect) as verified
 * on 2026-09-07:
 * <ul>
 *   <li>{@code token_hash VARCHAR(255)} — was {@code VARCHAR(64)} in the old
 *       untracked V1 file.</li>
 *   <li>{@code issued_at} — replaces the old {@code created_at} column that
 *       never existed in the live DB.</li>
 *   <li>{@code replaced_by_token_id UUID} — rotation audit trail: the old
 *       token's {@code replaced_by_token_id} is set to the new token's
 *       {@code id} during a {@code /refresh} call.</li>
 * </ul>
 *
 * <p>Only a SHA-256 hash of the actual token is stored, per
 * security-contract.md's "store only a secure hash" requirement.
 * The raw token value lives in the HTTP response for the session
 * duration and is never persisted in plain text.
 *
 * <p>{@code revokedAt} is null while the token is valid. Logout (and
 * rotation) sets this field. Expiry is checked by comparing
 * {@code expiresAt} against {@code Instant.now()}.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** SHA-256 hex digest of the raw token value. VARCHAR(255) in DB. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Timestamp when this token was issued. Set automatically by Hibernate
     * on first persist. Maps to the {@code issued_at} column in the DB
     * (there is no {@code created_at} column on this table).
     */
    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Non-null when the token has been explicitly revoked (logout, rotation). */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Rotation audit trail. Set to the new token's {@code id} when this
     * token is retired during a {@code /refresh} call.
     * {@code null} while this token has not been replaced yet.
     * References {@code refresh_tokens(id) ON DELETE SET NULL} in the DB.
     */
    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshToken() {
        // JPA
    }

    public RefreshToken(String tokenHash, UUID userId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
        // issuedAt is populated by @CreationTimestamp on first persist
        // replacedByTokenId is null until this token is rotated
    }

    public UUID getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public UUID getUserId() { return userId; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getReplacedByTokenId() { return replacedByTokenId; }

    public boolean isValid() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    /**
     * Sets the rotation link: points this (now-revoked) token to its
     * replacement. Called during {@code /refresh} after the new token
     * has been persisted and its ID is known.
     */
    public void setReplacedByTokenId(UUID newTokenId) {
        this.replacedByTokenId = newTokenId;
    }
}
