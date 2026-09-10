package com.visionselect.backend.api.internal;

import com.visionselect.backend.ai.dto.AiCallbackRequest;
import com.visionselect.backend.ai.dto.AiProgressRequest;
import com.visionselect.backend.ai.service.AiJobService;
import com.visionselect.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Internal callback endpoint for the Python AI service.
 *
 * <p>This endpoint is NOT part of the public REST API. It is protected
 * exclusively by the {@code X-Internal-Service-Token} header, validated by
 * {@link com.visionselect.backend.config.InternalServiceTokenFilter} before
 * this controller is reached.
 *
 * <p>JWT authentication is NOT used here. The endpoint is under
 * {@code /internal/**} which is {@code permitAll()} in SecurityConfig
 * (protection is delegated to the service-token filter, not the JWT filter).
 *
 * <p>DO NOT add JWT-based @PreAuthorize annotations to these methods.
 */
@RestController
@RequestMapping("/internal/v1/ai/jobs")
public class InternalAiCallbackController {

    private final AiJobService aiJobService;

    public InternalAiCallbackController(AiJobService aiJobService) {
        this.aiJobService = aiJobService;
    }

    /**
     * Receives the final result (COMPLETED or FAILED) from the Python AI service.
     *
     * <p>Repeated identical callbacks are idempotent \u2014 duplicate results are ignored.
     * The job must be in PROCESSING state; otherwise returns 409.
     */
    @PostMapping("/{jobId}/callback")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> callback(
            @PathVariable UUID jobId,
            @Valid @RequestBody AiCallbackRequest request) {
        aiJobService.handleCallback(jobId, request);
        return ApiResponse.success(null, "Callback processed");
    }

    /**
     * Receives a progress update (0\u2013100%) from the Python AI service.
     */
    @PostMapping("/{jobId}/progress")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> progress(
            @PathVariable UUID jobId,
            @Valid @RequestBody AiProgressRequest request) {
        aiJobService.updateProgress(jobId, request.progressPct());
        return ApiResponse.success(null, "Progress updated");
    }
}
