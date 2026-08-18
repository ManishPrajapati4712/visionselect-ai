package com.visionselect.backend.storage.local;

import com.visionselect.backend.storage.StorageObjectMetadata;
import com.visionselect.backend.storage.StorageProvider;
import com.visionselect.backend.storage.UploadGrant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stands in for a real cloud object-storage provider (S3, GCS) in local
 * dev and demo environments, per storage-contract.md: "which provider
 * loads is environment configuration." This is the only implementation
 * that exists as of this phase - a production deployment would configure
 * an S3/GCS-backed {@link StorageProvider} instead, not modify this one.
 *
 * <p>Because there is no real cloud endpoint to sign a URL against
 * locally, "issuing a signed upload URL" here means minting a random,
 * single-use, time-limited token and pointing the client at this backend's
 * own {@link LocalStorageUploadController}, which enforces that token the
 * same way a real provider's signature would - content-type match, size
 * ceiling, expiry - before writing bytes to {@link
 * LocalStorageProperties#baseDirectory()}. The backend receiving upload
 * bytes at all is specific to this dev stand-in; storage-contract.md's
 * "backend never receives raw video bytes" guarantee describes the real
 * provider path (client PUTs directly to S3/GCS), not this local shim.
 *
 * <p>Token/metadata state lives in memory only ({@link ConcurrentHashMap}),
 * not on disk or in the database - acceptable because this provider only
 * ever runs as a single local instance for development/demo, never as a
 * deployed replica set. A restart loses in-flight upload grants (an
 * abandoned upload attempt, the same failure mode storage-contract.md
 * already treats as expected housekeeping) and the content-type cache for
 * already-uploaded objects (falls back to a best-effort probe - see
 * {@link #find}).
 */
@Service
public class LocalStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageProvider.class);

    private final Path baseDirectory;
    private final String publicBaseUrl;

    private final Map<String, PendingUpload> pendingUploads = new ConcurrentHashMap<>();
    private final Map<String, PendingDownload> pendingDownloads = new ConcurrentHashMap<>();
    /** Best-effort cache of the content-type an object was uploaded with; see class javadoc. */
    private final Map<String, String> uploadedContentTypes = new ConcurrentHashMap<>();

    public LocalStorageProvider(LocalStorageProperties properties) {
        this.baseDirectory = Path.of(properties.baseDirectory()).toAbsolutePath().normalize();
        this.publicBaseUrl = properties.publicBaseUrl().replaceAll("/+$", "");
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create local storage directory: " + baseDirectory, e);
        }
    }

    @Override
    public UploadGrant issueUploadUrl(String key, String contentType, long maxSizeBytes, Duration expiry) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(expiry);
        pendingUploads.put(token, new PendingUpload(key, contentType, maxSizeBytes, expiresAt));

        String uploadUrl = publicBaseUrl + "/local-storage/upload/" + token;
        return new UploadGrant(uploadUrl, expiresAt, Map.of("Content-Type", contentType));
    }

    @Override
    public String issueDownloadUrl(String key, Duration expiry) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(expiry);
        pendingDownloads.put(token, new PendingDownload(key, expiresAt));
        return publicBaseUrl + "/local-storage/download/" + token;
    }

    @Override
    public Optional<StorageObjectMetadata> find(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            long size = Files.size(path);
            String contentType = uploadedContentTypes.getOrDefault(key, probeContentType(path));
            return Optional.of(new StorageObjectMetadata(size, contentType));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read local storage object: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete local storage object: " + key, e);
        }
        uploadedContentTypes.remove(key);
    }

    /**
     * Consumes an upload token minted by {@link #issueUploadUrl}, writing
     * {@code body} to disk if - and only if - the token is unexpired, the
     * declared content-type matches what the grant was issued for, and
     * {@code declaredContentLength} does not exceed the grant's ceiling.
     * Single-use: the token is removed whether this call succeeds or fails,
     * mirroring a real signed URL's one-shot nature closely enough for
     * dev/demo purposes.
     *
     * @throws LocalUploadTokenException if the token is missing/expired,
     *                                    the content-type doesn't match, or
     *                                    the declared size exceeds the grant
     */
    String receiveUpload(String token, String declaredContentType, long declaredContentLength, InputStream body) {
        PendingUpload pending = pendingUploads.remove(token);
        if (pending == null) {
            throw new LocalUploadTokenException("Upload token not found or already used");
        }
        if (Instant.now().isAfter(pending.expiresAt())) {
            throw new LocalUploadTokenException("Upload token has expired");
        }
        if (declaredContentType == null || !declaredContentType.equals(pending.contentType())) {
            throw new LocalUploadTokenException(
                    "Content-Type does not match the type this upload URL was issued for");
        }
        if (declaredContentLength > pending.maxSizeBytes()) {
            throw new LocalUploadTokenException("Upload exceeds the size limit this URL was issued for");
        }

        Path destination = resolve(pending.key());
        try {
            Files.createDirectories(destination.getParent());
            long written = Files.copy(body, destination, StandardCopyOption.REPLACE_EXISTING);
            if (written > pending.maxSizeBytes()) {
                Files.deleteIfExists(destination);
                throw new LocalUploadTokenException("Upload exceeds the size limit this URL was issued for");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write local storage object: " + pending.key(), e);
        }

        uploadedContentTypes.put(pending.key(), pending.contentType());
        log.debug("Local storage received upload for key={}", pending.key());
        return pending.key();
    }

    /** Resolves a download token minted by {@link #issueDownloadUrl} to a readable path. */
    Path resolveDownload(String token) {
        PendingDownload pending = pendingDownloads.get(token);
        if (pending == null) {
            throw new LocalUploadTokenException("Download token not found");
        }
        if (Instant.now().isAfter(pending.expiresAt())) {
            pendingDownloads.remove(token);
            throw new LocalUploadTokenException("Download token has expired");
        }
        Path path = resolve(pending.key());
        if (!Files.isRegularFile(path)) {
            throw new LocalUploadTokenException("Object no longer exists");
        }
        return path;
    }

    /**
     * Resolves a storage key to a path strictly inside {@link
     * #baseDirectory}, rejecting anything that would escape it. Defense in
     * depth: the key is generated by {@code VideoService}, never taken
     * from a client, but a local-disk provider is exactly the kind of
     * implementation where a path-traversal bug would be silently
     * catastrophic, so this is checked here regardless of caller trust.
     */
    private Path resolve(String key) {
        Path candidate = baseDirectory.resolve(key).normalize();
        if (!candidate.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Storage key resolves outside the storage root: " + key);
        }
        return candidate;
    }

    private static String probeContentType(Path path) {
        try {
            String probed = Files.probeContentType(path);
            return probed != null ? probed : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private record PendingUpload(String key, String contentType, long maxSizeBytes, Instant expiresAt) {
    }

    private record PendingDownload(String key, Instant expiresAt) {
    }
}
