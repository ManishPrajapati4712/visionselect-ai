package com.visionselect.backend.ai.entity;

/**
 * Six-state lifecycle for an AI analysis job.
 *
 * <p>Allowed transitions (enforced by {@link com.visionselect.backend.ai.service.AiJobService}):
 * <ul>
 *   <li>QUEUED      → PROCESSING (dispatcher picks up job)</li>
 *   <li>QUEUED      → CANCELLED  (user requests cancellation)</li>
 *   <li>PROCESSING  → COMPLETED  (Python callback — success)</li>
 *   <li>PROCESSING  → FAILED     (Python callback — error or timeout)</li>
 *   <li>FAILED      → RETRYING   (attempt_count &lt; max_attempts)</li>
 *   <li>RETRYING    → PROCESSING (retry window elapsed)</li>
 *   <li>RETRYING    → FAILED     (attempt_count >= max_attempts — terminal)</li>
 * </ul>
 *
 * <p>PROCESSING → CANCELLED is NOT allowed; in-flight jobs cannot be cancelled.
 */
public enum AiJobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRYING,
    CANCELLED;

    /** Returns true if this status is a terminal state (no further transitions possible). */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Returns true if this status allows a new submission for the same video+type.
     * Active statuses block new submissions via the partial unique index.
     */
    public boolean isActive() {
        return this == QUEUED || this == PROCESSING || this == RETRYING;
    }

    /** Validates that a transition from {@code current} to {@code next} is allowed. */
    public static void assertTransitionAllowed(AiJobStatus current, AiJobStatus next) {
        boolean allowed = switch (current) {
            case QUEUED     -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == COMPLETED  || next == FAILED;
            case FAILED     -> next == RETRYING;
            case RETRYING   -> next == PROCESSING || next == FAILED;
            case COMPLETED, CANCELLED -> false; // terminal
        };
        if (!allowed) {
            throw new IllegalStateException(
                "Invalid AI job state transition: " + current + " → " + next);
        }
    }
}
