package com.visionselect.backend.video.service;

import com.visionselect.backend.storage.StorageObjectMetadata;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VideoServiceTest {

    private VideoRepository videoRepository;
    private StorageProvider storageProvider;
    private VideoService videoService;

    @BeforeEach
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        storageProvider = mock(StorageProvider.class);
        videoService = new VideoService(videoRepository, storageProvider, 15);
    }

    @Test
    void requestUploadUrlGeneratesABackendOwnedKeyUnderTheRequestingUsersPrefix() {
        UUID userId = UUID.randomUUID();
        UploadUrlRequest request = new UploadUrlRequest("match.mp4", "video/mp4", 1_048_576L, null);
        UploadGrant grant = new UploadGrant(
                "http://localhost:8080/local-storage/upload/token-123",
                Instant.parse("2026-08-15T12:15:00Z"),
                Map.of("Content-Type", "video/mp4"));
        when(storageProvider.issueUploadUrl(any(), any(), anyLong(), any())).thenReturn(grant);

        UploadUrlResponse response = videoService.requestUploadUrl(userId, request);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageProvider).issueUploadUrl(keyCaptor.capture(), eq("video/mp4"), eq(1_048_576L), eq(Duration.ofMinutes(15)));

        String generatedKey = keyCaptor.getValue();
        assertThat(generatedKey).startsWith("videos/" + userId + "/");
        assertThat(generatedKey).endsWith(".mp4");
        assertThat(response.storageKey()).isEqualTo(generatedKey);
        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.uploadUrl()).isEqualTo(grant.uploadUrl());
    }

    @Test
    void requestUploadUrlUsesTheMovExtensionForQuicktime() {
        UUID userId = UUID.randomUUID();
        UploadUrlRequest request = new UploadUrlRequest("match.mov", "video/quicktime", 2048L, null);
        when(storageProvider.issueUploadUrl(any(), any(), anyLong(), any()))
                .thenReturn(new UploadGrant("http://x/upload/t", Instant.now(), Map.of()));

        UploadUrlResponse response = videoService.requestUploadUrl(userId, request);

        assertThat(response.storageKey()).endsWith(".mov");
    }

    @Test
    void requestUploadUrlWithAnUnsupportedMimeTypeIsRejectedBeforeTouchingStorage() {
        UUID userId = UUID.randomUUID();
        UploadUrlRequest request = new UploadUrlRequest("clip.avi", "video/x-msvideo", 2048L, null);

        assertThatThrownBy(() -> videoService.requestUploadUrl(userId, request))
                .isInstanceOf(UnsupportedVideoFormatException.class);

        verifyNoInteractions(storageProvider);
    }

    @Test
    void registerUploadCreatesAnUploadedRowWhenTheObjectExistsInStorage() {
        UUID userId = UUID.randomUUID();
        String storageKey = "videos/" + userId + "/abc.mp4";
        VideoCreateRequest request = new VideoCreateRequest(storageKey, "match.mp4", "video/mp4", 1_048_576L, null);

        when(videoRepository.findByStorageKey(storageKey)).thenReturn(Optional.empty());
        when(storageProvider.find(storageKey)).thenReturn(Optional.of(new StorageObjectMetadata(1_048_576L, "video/mp4")));
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VideoResponse response = videoService.registerUpload(userId, request);

        ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).save(videoCaptor.capture());
        Video saved = videoCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(VideoStatus.UPLOADED);
        assertThat(saved.getUploadedBy()).isEqualTo(userId);
        assertThat(saved.getStorageKey()).isEqualTo(storageKey);
        assertThat(response.status()).isEqualTo("UPLOADED");
    }

    @Test
    void registerUploadFailsWithNotFoundWhenTheObjectIsMissingFromStorage() {
        UUID userId = UUID.randomUUID();
        String storageKey = "videos/" + userId + "/never-uploaded.mp4";
        VideoCreateRequest request = new VideoCreateRequest(storageKey, "match.mp4", "video/mp4", 1_048_576L, null);

        when(videoRepository.findByStorageKey(storageKey)).thenReturn(Optional.empty());
        when(storageProvider.find(storageKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.registerUpload(userId, request))
                .isInstanceOf(VideoObjectNotFoundException.class);

        verify(videoRepository, never()).save(any());
    }

    @Test
    void registerUploadFailsWithConflictWhenTheStorageKeyIsAlreadyRegistered() {
        UUID userId = UUID.randomUUID();
        String storageKey = "videos/" + userId + "/dup.mp4";
        VideoCreateRequest request = new VideoCreateRequest(storageKey, "match.mp4", "video/mp4", 1_048_576L, null);
        Video existing = new Video("match.mp4", storageKey, "video/mp4", 1_048_576L, VideoStatus.UPLOADED, userId, null);

        when(videoRepository.findByStorageKey(storageKey)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> videoService.registerUpload(userId, request))
                .isInstanceOf(VideoAlreadyRegisteredException.class);

        verifyNoInteractions(storageProvider);
        verify(videoRepository, never()).save(any());
    }

    @Test
    void registerUploadWithAnUnsupportedMimeTypeIsRejectedBeforeAnyLookup() {
        UUID userId = UUID.randomUUID();
        VideoCreateRequest request = new VideoCreateRequest("videos/x/y.avi", "clip.avi", "video/x-msvideo", 2048L, null);

        assertThatThrownBy(() -> videoService.registerUpload(userId, request))
                .isInstanceOf(UnsupportedVideoFormatException.class);

        verifyNoInteractions(videoRepository, storageProvider);
    }
}
