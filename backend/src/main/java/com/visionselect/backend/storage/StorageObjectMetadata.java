package com.visionselect.backend.storage;

/**
 * Result of {@link StorageProvider#find(String)} when an object exists at
 * the given key. Matches the "Confirm existence" row of storage-contract.md's
 * responsibility table: boolean-ish presence plus basic metadata, nothing
 * more - this is a HEAD-equivalent check, not a content read.
 */
public record StorageObjectMetadata(
        long sizeBytes,
        String contentType
) {
}
