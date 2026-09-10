package com.visionselect.backend.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sends AI job requests to the Python FastAPI microservice via HTTP.
 *
 * <p>The Python service must respond immediately with 202 Accepted and then
 * POST the result asynchronously to the {@code callbackUrl}.
 *
 * <p>Configuration (via environment variables or application.yml):
 * <ul>
 *   <li>{@code app.ai.python-service-url} \u2014 base URL of the Python service</li>
 *   <li>{@code app.security.internal.service-token} \u2014 shared service token</li>
 * </ul>
 *
 * <p>NEVER log the service token or signed URLs.
 */
@Component
public class PythonAiProviderClient implements AiProviderClient {

    private static final Logger log = LoggerFactory.getLogger(PythonAiProviderClient.class);

    private final RestTemplate restTemplate;
    private final String pythonServiceUrl;
    private final String serviceToken;

    public PythonAiProviderClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.ai.python-service-url:http://localhost:8000}") String pythonServiceUrl,
            @Value("${app.security.internal.service-token}") String serviceToken) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.pythonServiceUrl = pythonServiceUrl.replaceAll("/+$", "");
        this.serviceToken = serviceToken;
    }

    @Override
    public void submitJob(UUID jobId, UUID videoId, UUID playerId,
                          String signedVideoUrl, String jobType,
                          String callbackUrl, String progressUrl,
                          int attemptNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Service-to-service authentication — token must NOT be logged
        headers.set("X-Internal-Service-Token", serviceToken);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("job_id", jobId.toString());
        body.put("video_id", videoId.toString());
        body.put("player_id", playerId.toString());
        body.put("job_type", jobType);
        body.put("video_url", signedVideoUrl); // signed URL — do not log
        body.put("callback_url", callbackUrl);
        body.put("progress_url", progressUrl);
        body.put("attempt_number", attemptNumber);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Dispatching AI job jobId={} videoId={} playerId={} jobType={} attempt={}",
                jobId, videoId, playerId, jobType, attemptNumber);

        restTemplate.postForEntity(pythonServiceUrl + "/analyze", request, Void.class);
        log.info("AI job dispatched successfully jobId={}", jobId);
    }
}
