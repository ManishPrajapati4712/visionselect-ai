package com.visionselect.backend.api.v1.ai;

import com.visionselect.backend.ai.dto.AiAnalysisResultResponse;
import com.visionselect.backend.ai.dto.AiJobCreateRequest;
import com.visionselect.backend.ai.dto.AiJobResponse;
import com.visionselect.backend.ai.entity.AiAnalysisResult;
import com.visionselect.backend.ai.repository.AiAnalysisResultRepository;
import com.visionselect.backend.ai.service.AiJobService;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.response.ApiResponse;
import com.visionselect.backend.common.util.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for the V4 AI Job Pipeline.
 *
 * <p>RBAC (enforced via {@code @PreAuthorize}):
 * <ul>
 *   <li>POST   /ai/jobs         \u2014 ADMIN, COACH</li>
 *   <li>GET    /ai/jobs/{id}    \u2014 ADMIN, COACH, SELECTOR</li>
 *   <li>GET    /ai/jobs/{id}/result \u2014 ADMIN, COACH, SELECTOR</li>
 *   <li>POST   /ai/jobs/{id}/cancel \u2014 ADMIN, COACH</li>
 * </ul>
 *
 * <p>All responses use the standard {@link ApiResponse} envelope.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/ai/jobs")
public class AiJobController {

    private final AiJobService aiJobService;
    private final AiAnalysisResultRepository resultRepository;

    public AiJobController(AiJobService aiJobService,
                           AiAnalysisResultRepository resultRepository) {
        this.aiJobService = aiJobService;
        this.resultRepository = resultRepository;
    }

    /**
     * Submit a new AI analysis job.
     * Requires an {@code Idempotency-Key} value in the request body.
     * ADMIN and COACH only; COACH can only submit for videos they uploaded.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<AiJobResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AiJobCreateRequest request) {
        return ApiResponse.success(aiJobService.submit(principal, request), "AI job queued");
    }

    /**
     * Get the current status and progress of a job.
     * ADMIN, COACH (own jobs), SELECTOR (read-only).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COACH','SELECTOR')")
    public ApiResponse<AiJobResponse> getStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(aiJobService.getById(id, principal));
    }

    /**
     * Get the analysis result for a completed job.
     * ADMIN, COACH, SELECTOR.
     */
    @GetMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN','COACH','SELECTOR')")
    public ApiResponse<AiAnalysisResultResponse> getResult(@PathVariable UUID id) {
        AiAnalysisResult result = resultRepository.findByAiJobId(id)
                .orElseThrow(() -> new AiJobService.AiJobNotFoundException(
                        "No result found for job: " + id));
        return ApiResponse.success(AiAnalysisResultResponse.from(result));
    }

    /**
     * Cancel a QUEUED job.
     * ADMIN can cancel any; COACH can cancel only their own.
     * PROCESSING jobs cannot be cancelled.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<AiJobResponse> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(aiJobService.cancel(id, principal), "Job cancelled");
    }
}
