package com.visionselect.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain exceptions that map to a specific HTTP status.
 *
 * <p>Subclasses supply the HTTP status, a machine-readable error code, a
 * human-readable message, and an optional field name (for field-level errors
 * like unsupported MIME types or duplicate storage keys). The
 * {@link GlobalExceptionHandler} catches this type and translates it into
 * the standard {@link com.visionselect.backend.common.response.ApiResponse}
 * envelope.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String field; // nullable — present for field-specific errors

    protected ApiException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.field = null;
    }

    protected ApiException(HttpStatus httpStatus, String errorCode, String message, String field) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.field = field;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getField() {
        return field;
    }
}
