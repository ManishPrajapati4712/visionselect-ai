package com.visionselect.backend.video.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Step 1 response. Matches {@code UploadUrlResponse} in openapi.yaml.
 * {@code method} is always {@code "PUT"} per the schema's single-value
 * enum - fixed here rather than threaded through from {@link
 * com.visionselect.backend.storage.StorageProvider}, since every provider
 * this contract supports uses a signed PUT.
 */
public record UploadUrlResponse(
        String storageKey,
        String uploadUrl,
        String method,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
    public static UploadUrlResponse of(String storageKey, String uploadUrl, Instant expiresAt,
                                        Map<String, String> requiredHeaders) {
        return new UploadUrlResponse(storageKey, uploadUrl, "PUT", expiresAt, requiredHeaders);
    }
}
