package com.visionselect.backend.ai.provider;

import java.util.UUID;

/**
 * Provider-agnostic contract for submitting and cancelling AI analysis jobs.
 *
 * <p>Implementations must be replaceable without changes to {@code AiJobService}.
 * The first implementation is {@link PythonAiProviderClient} (HTTP to Python FastAPI).
 * {@link MockAiProviderClient} is used in tests and local dev without a running Python service.
 *
 * <p>Implementations MUST NOT call Gemini or any AI model directly from Spring Boot.
 * All model calls happen inside the Python service.
 */
public interface AiProviderClient {

    /**
     * Submits an analysis job to the AI provider.
     *
     * <p>The provider MUST respond immediately (202-style) and perform the work
     * asynchronously, calling back to {@code callbackUrl} when done.
     *
     * @param jobId          the Spring-side job UUID
     * @param videoId        the video to analyse
     * @param playerId       the player profile
     * @param signedVideoUrl short-lived URL the provider uses to fetch the video
     * @param jobType        what aspect to analyse (FULL_ANALYSIS etc.)
     * @param callbackUrl    where the provider POSTs the result
     * @param progressUrl    where the provider POSTs progress updates
     * @param attemptNumber  current attempt count (1-based)
     */
    void submitJob(UUID jobId, UUID videoId, UUID playerId,
                   String signedVideoUrl, String jobType,
                   String callbackUrl, String progressUrl,
                   int attemptNumber);
}
