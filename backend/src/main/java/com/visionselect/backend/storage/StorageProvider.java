package com.visionselect.backend.storage;

import java.time.Duration;
import java.util.Optional;

/**
 * The provider-agnostic contract from storage-contract.md. Exactly four
 * operations, all scoped to a single object key:
 * <ul>
 *   <li>{@link #issueUploadUrl} - issue upload access</li>
 *   <li>{@link #issueDownloadUrl} - issue download access</li>
 *   <li>{@link #find} - confirm existence</li>
 *   <li>{@link #delete} - delete</li>
 * </ul>
 *
 * <p>A {@code StorageProvider} never makes authorization decisions (the
 * caller - {@code video} module business logic - decides whether this
 * request is allowed before ever calling here) and never touches the
 * database. It also never invents the object key: every method here is
 * handed a key the caller already generated
 * ({@code videos/{userId}/{uuid}.{ext}} per storage-contract.md), never a
 * client-supplied path.
 *
 * <p>Which implementation is wired up ({@link
 * com.visionselect.backend.storage.local.LocalStorageProvider} today; a
 * future {@code S3StorageProvider} / {@code GoogleCloudStorageProvider})
 * is environment configuration, never a branch inside {@code video} or
 * {@code jobs} module logic - that's the actual test of whether this
 * abstraction is doing its job.
 */
public interface StorageProvider {

    /**
     * Issue a signed PUT URL scoped to {@code key}, with the content-type
     * and size constraint baked into the grant wherever the underlying
     * provider supports it.
     *
     * @param key           the storage key the caller generated, never
     *                      client-supplied
     * @param contentType   the exact MIME type the eventual upload must match
     * @param maxSizeBytes  the exact size ceiling the eventual upload must
     *                      not exceed
     * @param expiry        how long the returned grant remains valid
     */
    UploadGrant issueUploadUrl(String key, String contentType, long maxSizeBytes, Duration expiry);

    /**
     * Issue a short-lived signed GET URL scoped to {@code key}. Per
     * storage-contract.md, this is the only way any client ever reads an
     * object - raw storage credentials are never returned to a client.
     */
    String issueDownloadUrl(String key, Duration expiry);

    /**
     * Confirm whether an object exists at {@code key} and, if so, its
     * basic metadata. This is the HEAD-equivalent check {@code POST
     * /videos} uses before creating the {@code Video} row (api-contracts.md).
     */
    Optional<StorageObjectMetadata> find(String key);

    /**
     * Delete the object at {@code key}. Per storage-contract.md, physical
     * deletion is decoupled from the user-facing API call that triggers a
     * soft delete - this method backs the async cleanup process, not
     * {@code DELETE /videos/{videoId}} directly.
     */
    void delete(String key);
}
