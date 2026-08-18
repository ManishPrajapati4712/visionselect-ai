package com.visionselect.backend.video.exception;

import com.visionselect.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Maps to 409. Not an openapi.yaml-documented response for {@code POST
 * /videos} (only 400 and 404 are), but a straightforward safeguard: a
 * {@code storageKey} is unique per data-dictionary.md, and this catches a
 * double-submit (retry, duplicate tab, etc.) with a clear, specific error
 * instead of letting the database's unique-constraint violation fall
 * through to a generic 500.
 */
public class VideoAlreadyRegisteredException extends ApiException {

    public VideoAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "VIDEO_ALREADY_REGISTERED",
                "A video has already been registered for this storage key", "storageKey");
    }
}
