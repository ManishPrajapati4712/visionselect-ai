package com.visionselect.backend.video.exception;

import com.visionselect.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Maps to 415, per the {@code UnsupportedMediaType} response on {@code
 * POST /videos/upload-url} in openapi.yaml. Raised before a signed upload
 * URL is ever issued - per security-contract.md's file-validation
 * section, "a rejected upload never touches storage at all."
 */
public class UnsupportedVideoFormatException extends ApiException {

    public UnsupportedVideoFormatException(String mimeType) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported video format: " + mimeType + ". Only video/mp4 and video/quicktime are accepted.",
                "mimeType");
    }
}
