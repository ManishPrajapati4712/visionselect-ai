package com.visionselect.backend.storage;

import java.time.Instant;
import java.util.Map;

/**
 * What a {@link StorageProvider} hands back for "issue upload access":
 * a signed URL the client can {@code PUT} to directly, any headers the
 * client must send along with it (e.g. a required {@code Content-Type} so
 * the signature/policy actually constrains what gets uploaded), and when
 * the grant expires. Matches {@code UploadUrlResponse} in openapi.yaml
 * one-to-one; {@link com.visionselect.backend.video.service.VideoService}
 * maps this directly into that DTO alongside the storage key it generated.
 */
public record UploadGrant(
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
