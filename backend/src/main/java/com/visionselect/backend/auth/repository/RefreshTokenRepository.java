package com.visionselect.backend.auth.repository;

import com.visionselect.backend.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoke all active tokens for a user — used on logout to invalidate all sessions. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId);

    /** Delete expired and revoked tokens for a user (housekeeping). */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId AND (rt.revokedAt IS NOT NULL OR rt.expiresAt < CURRENT_TIMESTAMP)")
    void deleteExpiredAndRevokedByUserId(@Param("userId") UUID userId);
}
