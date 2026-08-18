package com.visionselect.backend.storage.local;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * The dev/demo stand-in for "the object storage provider's own endpoint"
 * - i.e. what {@code S3}/{@code GCS} would be if this weren't a local
 * shim. A client that received an {@code UploadGrant} from {@link
 * LocalStorageProvider} {@code PUT}s bytes here; a client that received a
 * download URL {@code GET}s from here.
 *
 * <p>Deliberately outside {@link com.visionselect.backend.common.util.ApiPaths#V1}
 * - this is not a business endpoint of the VisionSelect API and does not
 * appear in openapi.yaml, the same reasoning that keeps {@code /internal}
 * out of the versioned prefix. It is also deliberately outside JWT
 * authentication (see {@code SecurityConfig}'s permit list): a real signed
 * cloud URL doesn't carry this backend's bearer token either, its own
 * signature/token *is* the credential, and that's the property this
 * controller is standing in for via {@link LocalStorageProvider}'s
 * token checks.
 */
@RestController
public class LocalStorageUploadController {

    private final LocalStorageProvider localStorageProvider;

    public LocalStorageUploadController(LocalStorageProvider localStorageProvider) {
        this.localStorageProvider = localStorageProvider;
    }

    @PutMapping("/local-storage/upload/{token}")
    public ResponseEntity<Void> upload(@PathVariable String token,
                                        @RequestHeader(value = "Content-Type", required = false) String contentType,
                                        HttpServletRequest request) throws IOException {
        long declaredLength = request.getContentLengthLong();
        try (InputStream body = request.getInputStream()) {
            localStorageProvider.receiveUpload(token, contentType, declaredLength, body);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/local-storage/download/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        Path path = localStorageProvider.resolveDownload(token);
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Token problems (missing/expired/mismatched) map to plain-text 4xx
     * responses, not the {@code ApiResponse} envelope - this controller
     * simulates a cloud provider's own endpoint, which has its own error
     * shape entirely unrelated to this backend's API contract.
     */
    @ExceptionHandler(LocalUploadTokenException.class)
    public ResponseEntity<String> handleTokenError(LocalUploadTokenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
