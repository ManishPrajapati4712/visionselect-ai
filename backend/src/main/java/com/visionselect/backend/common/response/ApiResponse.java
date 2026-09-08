package com.visionselect.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard API response envelope used by all backend endpoints.
 *
 * <p>Shape (matches both existing test assertions and frontend ApiResponse<T> type):
 * <pre>
 * {
 *   "success": true,
 *   "status":  "success",
 *   "data":    { ... },
 *   "message": "...",
 *   "errors":  []
 * }
 * </pre>
 *
 * <p>Tests assert {@code $.status} as a string ("success"/"error").
 * The frontend reads {@code envelope.success} as a boolean.
 * Both fields are emitted so both consumers are satisfied.
 *
 * <p>The {@code errors} array holds either:
 * <ul>
 *   <li>Field-validation objects: {@code {field, code, message}} — from
 *       {@code MethodArgumentNotValidException}</li>
 *   <li>Domain-error objects:     {@code {code, message}}          — from
 *       {@code ApiException}</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String status,
        T data,
        String message,
        List<Object> errors
) {
    /** Success response with data only. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "success", data, null, List.of());
    }

    /** Success response with data and a message. */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "success", data, message, List.of());
    }

    /** Error response with a message and optional error detail list. */
    public static <T> ApiResponse<T> error(String message, List<Object> errors) {
        return new ApiResponse<>(false, "error", null, message, errors);
    }

    /** Error response with just a message. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, "error", null, message, List.of());
    }
}
