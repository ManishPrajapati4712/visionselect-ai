package com.visionselect.backend.video.service;

import com.visionselect.backend.storage.StorageProvider;
import com.visionselect.backend.storage.UploadGrant;
import com.visionselect.backend.video.dto.UploadUrlRequest;
import com.visionselect.backend.video.dto.UploadUrlResponse;
import com.visionselect.backend.video.dto.VideoCreateRequest;
import com.visionselect.backend.video.dto.VideoResponse;
import com.visionselect.backend.video.entity.Video;
import com.visionselect.backend.video.entity.VideoStatus;
import com.visionselect.backend.video.exception.UnsupportedVideoFormatException;
import com.visionselect.backend.video.exception.VideoAlreadyRegisteredException;
import com.visionselect.backend.video.exception.VideoObjectNotFoundException;
import com.visionselect.backend.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Backs the two-step direct-to-storage upload flow from
 * storage-contract.md and the Videos tag in openapi.yaml:
 * {@code POST /videos/upload-url} ({@link #requestUploadUrl}) then
 * {@code POST /videos} ({@link #registerUpload}). Nothing else from that
 * tag ({@code GET /videos}, {@code GET}/{@code DELETE /videos/{videoId}})
 * is implemented in this phase.
 *
 * <p>Role gating (security-contract.md: only {@code ADMIN}/{@code COACH}
 * may upload/create) is enforced with {@code @PreAuthorize} on {@link
 * com.visionselect.backend.api.v1.videos.VideoController}, not here -
 * consistent with how {@code SecurityConfig}'s javadoc says RBAC narrowing
 * is applied at the controller/method level.
 */
@Service
public class VideoService {

    /** Exactly the two values {@code UploadUrlRequest.mimeType} is allowed per openapi.yaml. */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("video/mp4", "video/quicktime");

    private final VideoRepository videoRepository;
    private final StorageProvider storageProvider;
    private final Duration uploadUrlTtl;

    public VideoService(VideoRepository videoRepository,
                         StorageProvider storageProvider,
                         @Value("${app.video.upload-url-ttl-minutes:15}") long uploadUrlTtlMinutes) {
        this.videoRepository = videoRepository;
        this.storageProvider = storageProvider;
        this.uploadUrlTtl = Duration.ofMinutes(uploadUrlTtlMinutes);
    }

    /** Step 1: authorize an upload and hand back a signed direct-to-storage URL. No DB row is created here. */
    public UploadUrlResponse requestUploadUrl(UUID requestingUserId, UploadUrlRequest request) {
        validateMimeType(request.mimeType());
        // fileSizeBytes range (positive, <= 500MB) is already enforced by
        // @Valid on the DTO; re-checking here would just duplicate that.

        String storageKey = generateStorageKey(requestingUserId, request.mimeType());
        UploadGrant grant = storageProvider.issueUploadUrl(
                storageKey, request.mimeType(), request.fileSizeBytes(), uploadUrlTtl);

        return UploadUrlResponse.of(storageKey, grant.uploadUrl(), grant.expiresAt(), grant.requiredHeaders());
    }

    /** Step 2: confirm the direct upload actually landed in storage, then persist the metadata row. */
    public VideoResponse registerUpload(UUID requestingUserId, VideoCreateRequest request) {
        validateMimeType(request.mimeType());

        if (videoRepository.findByStorageKey(request.storageKey()).isPresent()) {
            throw new VideoAlreadyRegisteredException();
        }

        // The HEAD-equivalent check api-contracts.md requires before ever
        // writing a row: a storageKey with nothing behind it means the
        // client's direct PUT never completed (or this call arrived before
        // it did).
        storageProvider.find(request.storageKey())
                .orElseThrow(VideoObjectNotFoundException::new);

        Video video = new Video(
                request.filename(),
                request.storageKey(),
                request.mimeType(),
                request.fileSizeBytes(),
                VideoStatus.UPLOADED,
                requestingUserId,
                request.playerId()
        );
        video = videoRepository.save(video);

        return VideoResponse.from(video);
    }

    private void validateMimeType(String mimeType) {
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new UnsupportedVideoFormatException(mimeType);
        }
    }

    /**
     * {@code videos/{userId}/{uuid}.{ext}}, per storage-contract.md's key
     * layout. Backend-generated end to end - a client never supplies or
     * influences any part of this key.
     */
    private String generateStorageKey(UUID userId, String mimeType) {
        String extension = EXTENSIONS_BY_MIME_TYPE.getOrDefault(mimeType, "bin");
        return "videos/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    private static final Map<String, String> EXTENSIONS_BY_MIME_TYPE = Map.of(
            "video/mp4", "mp4",
            "video/quicktime", "mov"
    );
}
