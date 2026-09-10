package com.visionselect.backend.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock AI provider used in unit/controller tests and local dev without a running Python service.
 *
 * <p>This implementation does nothing (no HTTP call). In tests the {@code AiJobService}
 * is mocked directly; this class exists so the Spring context can wire the interface
 * without needing the Python service URL to be reachable.
 *
 * <p>Annotated {@code @Primary} on the {@code test} profile so MockMvc tests that DO
 * load a Spring context pick this over {@link PythonAiProviderClient}.
 */
@Component
@Profile("test")
@Primary
public class MockAiProviderClient implements AiProviderClient {

    private static final Logger log = LoggerFactory.getLogger(MockAiProviderClient.class);

    @Override
    public void submitJob(UUID jobId, UUID videoId, UUID playerId,
                          String signedVideoUrl, String jobType,
                          String callbackUrl, String progressUrl,
                          int attemptNumber) {
        log.info("[MOCK] AI job submitted jobId={} jobType={} attempt={} (no real HTTP call)",
                jobId, jobType, attemptNumber);
        // No-op: tests inject callbacks directly or mock AiJobService entirely.
    }
}
