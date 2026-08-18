package com.visionselect.backend.video.entity;

/**
 * Matches both the {@code CHECK IN ('PENDING_UPLOAD','UPLOADED','DELETED')}
 * constraint on {@code videos.status} in data-dictionary.md and the
 * {@code VideoStatus} enum in openapi.yaml exactly (same names, same
 * order).
 *
 * <p>{@code PENDING_UPLOAD} is the column's DB-level default but is never
 * actually assigned by {@link com.visionselect.backend.video.service.VideoService}
 * as of this phase: a {@code Video} row is only ever created by {@code
 * POST /videos}, which per api-contracts.md happens *after* the backend
 * has already confirmed the object exists in storage - so every row this
 * module creates starts life as {@link #UPLOADED}. {@code PENDING_UPLOAD}
 * exists in the enum/schema for a future flow (e.g. reserving a row at
 * upload-url time) that isn't part of this phase.
 */
public enum VideoStatus {
    PENDING_UPLOAD,
    UPLOADED,
    DELETED
}
