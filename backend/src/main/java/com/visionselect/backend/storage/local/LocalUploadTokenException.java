package com.visionselect.backend.storage.local;

/**
 * Raised by {@link LocalStorageProvider} when an upload/download token is
 * missing, expired, or the request doesn't match the grant it was issued
 * for. Caught by {@link LocalStorageUploadController} and translated into
 * an HTTP response - this is dev-shim plumbing, not part of the public
 * {@code /api/v1} contract, so it deliberately does not go through {@link
 * com.visionselect.backend.common.exception.ApiException} / the standard
 * envelope.
 */
class LocalUploadTokenException extends RuntimeException {

    LocalUploadTokenException(String message) {
        super(message);
    }
}
