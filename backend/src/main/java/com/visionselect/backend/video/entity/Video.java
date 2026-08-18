package com.visionselect.backend.video.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps exactly to the {@code videos} table in data-dictionary.md: a
 * pointer to object storage plus upload/ownership metadata. Never holds
 * video bytes, and {@code durationSeconds} is deliberately not
 * client-settable anywhere in this entity's public API - see the field's
 * javadoc.
 */
@Entity
@Table(name = "videos")
public class Video {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 1000)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    /** Populated after AI preprocessing (a later phase) - never set from a request DTO. */
    @Column(name = "duration_seconds", precision = 8, scale = 2)
    private BigDecimal durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VideoStatus status;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    /** Nullable - no FK enforced at the JPA level yet; see V2 migration comment on the players module not existing. */
    @Column(name = "player_id")
    private UUID playerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Soft-delete marker per data-dictionary.md. Not acted on anywhere yet - no delete endpoint exists in this phase. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Video() {
        // JPA
    }

    public Video(String filename, String storageKey, String mimeType, long fileSizeBytes,
                 VideoStatus status, UUID uploadedBy, UUID playerId) {
        this.filename = filename;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = status;
        this.uploadedBy = uploadedBy;
        this.playerId = playerId;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public BigDecimal getDurationSeconds() {
        return durationSeconds;
    }

    public VideoStatus getStatus() {
        return status;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
