package com.visionselect.backend.storage.local;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.storage.local.*}. Dev/demo only - see
 * {@link LocalStorageProvider}'s javadoc for why this provider exists and
 * why it is deliberately not production-shaped.
 *
 * @param baseDirectory   filesystem directory objects are written under.
 *                        Created on startup if missing.
 * @param publicBaseUrl   the origin this backend is reachable at from the
 *                         browser doing the upload (e.g.
 *                         {@code http://localhost:8080}), used to build the
 *                         signed URL returned to the client. Distinct from
 *                         {@code server.port} alone because in a real
 *                         deployment this might sit behind a different
 *                         host/proxy path than the JVM's own bind address.
 * @param uploadGrantTtl  how long an issued upload URL remains valid, in
 *                         seconds.
 */
@ConfigurationProperties(prefix = "app.storage.local")
public record LocalStorageProperties(
        String baseDirectory,
        String publicBaseUrl,
        long uploadGrantTtlSeconds
) {
}
