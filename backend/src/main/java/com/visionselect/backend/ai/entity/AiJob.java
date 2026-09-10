package com.visionselect.backend.ai.entity;

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
 * Maps to the {@code ai_jobs} table created by the V4 Flyway migration.
 *
 * <p>Conventions follow V1–V3 entities:
 * <ul>
 *   <li>UUID primary key via {@code @UuidGenerator}.</li>
 *   <li>No {@code @ManyToOne} — foreign keys held as raw {@code UUID} fields.</li>
 *   <li>No {@code deleted_at}: cancelled/failed jobs are kept for audit (status = CANCELLED).</li>
 *   <li>Enum fields use {@code @Enumerated(EnumType.STRING)} matching the DB CHECK constraint values exactly.</li>
 *   <li>State transitions are validated by {@link AiJobStatus#assertTransitionAllowed}.</li>
 * </ul>
 */
@Entity
@Table(name = "ai_jobs")
public class AiJob {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false, updatable = false)
    private UUID videoId;

    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "submitted_by", nullable = false, updatable = false)
    private UUID submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, updatable = false, length = 30)
    private AiJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private AiProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiJobStatus status;

    @Column(name = "progress_pct")
    private Short progressPct;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private short maxAttempts = 3;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "callback_received_at")
    private Instant callbackReceivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiJob() {
        // JPA
    }

    /** Creates a new job in QUEUED state. */
    public AiJob(UUID videoId, UUID playerId, UUID submittedBy,
                 AiJobType jobType, AiProvider provider, String idempotencyKey, short maxAttempts) {
        this.videoId = videoId;
        this.playerId = playerId;
        this.submittedBy = submittedBy;
        this.jobType = jobType;
        this.provider = provider;
        this.idempotencyKey = idempotencyKey;
        this.status = AiJobStatus.QUEUED;
        this.maxAttempts = maxAttempts;
        this.queuedAt = Instant.now();
    }

    // ── State transition helpers ──────────────────────────────────────────────

    /** Transitions QUEUED or RETRYING → PROCESSING. Validates the transition first. */
    public void markProcessing() {
        AiJobStatus.assertTransitionAllowed(this.status, AiJobStatus.PROCESSING);
        this.status = AiJobStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.progressPct = 0;
    }

    /** Transitions PROCESSING → COMPLETED. */
    public void markCompleted() {
        AiJobStatus.assertTransitionAllowed(this.status, AiJobStatus.COMPLETED);
        this.status = AiJobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.callbackReceivedAt = Instant.now();
        this.progressPct = 100;
    }

    /** Transitions PROCESSING → FAILED. Sets error details. */
    public void markFailed(String errorCode, String errorMessage) {
        AiJobStatus.assertTransitionAllowed(this.status, AiJobStatus.FAILED);
        this.status = AiJobStatus.FAILED;
        this.failedAt = Instant.now();
        this.callbackReceivedAt = Instant.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.attemptCount++;
    }

    /** Transitions FAILED → RETRYING with exponential backoff next_retry_at. */
    public void scheduleRetry(Instant nextRetryAt) {
        AiJobStatus.assertTransitionAllowed(this.status, AiJobStatus.RETRYING);
        this.status = AiJobStatus.RETRYING;
        this.nextRetryAt = nextRetryAt;
    }

    /** Transitions QUEUED → CANCELLED. */
    public void markCancelled() {
        AiJobStatus.assertTransitionAllowed(this.status, AiJobStatus.CANCELLED);
        this.status = AiJobStatus.CANCELLED;
    }

    /** Updates progress percentage (0–100). Only valid while PROCESSING. */
    public void updateProgress(short progressPct) {
        this.progressPct = progressPct;
    }

    public boolean hasRetriesRemaining() {
        return this.attemptCount < this.maxAttempts;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public UUID getVideoId()             { return videoId; }
    public UUID getPlayerId()            { return playerId; }
    public UUID getSubmittedBy()         { return submittedBy; }
    public AiJobType getJobType()        { return jobType; }
    public AiProvider getProvider()      { return provider; }
    public AiJobStatus getStatus()       { return status; }
    public Short getProgressPct()        { return progressPct; }
    public short getAttemptCount()       { return attemptCount; }
    public short getMaxAttempts()        { return maxAttempts; }
    public String getErrorCode()         { return errorCode; }
    public String getErrorMessage()      { return errorMessage; }
    public String getIdempotencyKey()    { return idempotencyKey; }
    public Instant getQueuedAt()         { return queuedAt; }
    public Instant getStartedAt()        { return startedAt; }
    public Instant getCompletedAt()      { return completedAt; }
    public Instant getFailedAt()         { return failedAt; }
    public Instant getNextRetryAt()      { return nextRetryAt; }
    public Instant getCallbackReceivedAt() { return callbackReceivedAt; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }
}
