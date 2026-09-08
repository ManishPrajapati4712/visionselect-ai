package com.visionselect.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the {@code users} table created by V1 Flyway migration.
 *
 * <p>Column layout matches the live database (visionselect) as verified
 * on 2026-09-07:
 * <ul>
 *   <li>{@code full_name VARCHAR(255)} — was {@code display_name VARCHAR(100)}
 *       in the old untracked V1 file; corrected to match the live DB.</li>
 *   <li>{@code email VARCHAR(255)} — was {@code VARCHAR(320)} in the old file.</li>
 *   <li>{@code deleted_at} — soft-delete marker; present in the live DB,
 *       now mapped here.</li>
 * </ul>
 *
 * <p>No FK references to external tables here — other modules reference
 * {@code users(id)} (e.g. {@code videos.uploaded_by}) but never the
 * reverse, so no bidirectional JPA relationships are needed.
 *
 * <p>The API JSON field for the user's display name remains {@code displayName}
 * (see {@link com.visionselect.backend.auth.dto.AuthUserDto}) for frontend
 * compatibility; only the DB column and Java field names change here.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Stored in {@code full_name} column (VARCHAR 255). */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Soft-delete marker. {@code null} means the account is active. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
        // JPA
    }

    public User(String email, String passwordHash, String fullName, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    /** Returns the user's full name (stored as {@code full_name} in the DB). */
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    /** Returns the soft-delete timestamp, or {@code null} if the account is active. */
    public Instant getDeletedAt() { return deletedAt; }
}
