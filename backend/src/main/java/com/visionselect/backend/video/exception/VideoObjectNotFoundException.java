package com.visionselect.backend.video.exception;

import com.visionselect.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Maps to 404, per {@code POST /videos}'s documented response in
 * openapi.yaml: "storageKey not found in storage (upload never
 * completed)." Raised when the pre-create existence check ({@link
 * com.visionselect.backend.storage.StorageProvider#find}) comes back
 * empty - the client called this step before (or without) actually
 * completing the direct upload to storage.
 */
public class VideoObjectNotFoundException extends ApiException {

    public VideoObjectNotFoundException() {
        super(HttpStatus.NOT_FOUND, "STORAGE_OBJECT_NOT_FOUND",
                "No object was found at this storage key - the upload may not have completed yet",
                "storageKey");
    }
}
