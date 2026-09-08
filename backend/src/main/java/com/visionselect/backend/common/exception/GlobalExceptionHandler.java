package com.visionselect.backend.common.exception;

import com.visionselect.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates exceptions into the standard {@link ApiResponse} envelope for
 * every {@code @RestController} in the application.
 *
 * <p>Handlers (matched top-to-bottom):
 * <ol>
 *   <li>{@link ApiException} — maps to the HTTP status declared on the
 *       exception; error detail includes {@code code} and optional
 *       {@code field}.</li>
 *   <li>{@link MethodArgumentNotValidException} — maps to 400; each failed
 *       constraint produces a {@code {field, code, message}} entry.</li>
 *   <li>{@link HttpMessageNotReadableException} — maps to 400; catches unknown
 *       JSON properties (rejected by {@code @JsonIgnoreProperties(ignoreUnknown=false)})
 *       and missing request body / unparse-able JSON.</li>
 *   <li>Unhandled {@link Exception} — maps to 500 without leaking internal
 *       details to the client (the full stack trace is logged server-side).</li>
 * </ol>
 *
 * <p>Per the application config ({@code server.error.include-message: never}
 * etc.), Spring Boot's default error machinery never runs — this handler
 * owns the response shape entirely.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -----------------------------------------------------------------------
    // Domain exceptions
    // -----------------------------------------------------------------------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex,
                                                                 HttpServletRequest request) {
        log.debug("ApiException [{}] at {}: {}", ex.getErrorCode(), request.getRequestURI(), ex.getMessage());

        Map<String, Object> error = new LinkedHashMap<>();
        if (ex.getField() != null) {
            error.put("field", ex.getField());
        }
        error.put("code", ex.getErrorCode());
        error.put("message", ex.getMessage());

        ApiResponse<Void> body = ApiResponse.error(ex.getMessage(), List.of(error));
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    // -----------------------------------------------------------------------
    // Bean-validation failures (@Valid)
    // -----------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<Object> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("field", fieldError.getField());
            entry.put("code", fieldError.getCode() != null ? fieldError.getCode() : "INVALID");
            entry.put("message", fieldError.getDefaultMessage());
            errors.add(entry);
        }
        String message = "Validation failed: " + errors.size() + " error(s)";
        return ResponseEntity.badRequest().body(ApiResponse.error(message, errors));
    }

    // -----------------------------------------------------------------------
    // Unreadable / unknown-field JSON
    // -----------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = "Request body is missing or contains unrecognised fields";
        Map<String, String> error = Map.of("code", "INVALID_REQUEST_BODY", "message", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message, List.of(error)));
    }

    // -----------------------------------------------------------------------
    // Catch-all
    // -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex,
                                                               HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        String message = "An unexpected error occurred";
        Map<String, String> error = Map.of("code", "INTERNAL_ERROR", "message", message);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, List.of(error)));
    }
}
