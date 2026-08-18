package com.visionselect.backend.storage.local;

import com.visionselect.backend.storage.StorageObjectMetadata;
import com.visionselect.backend.storage.UploadGrant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageProviderTest {

    @TempDir
    Path tempDir;

    private LocalStorageProvider provider;

    @BeforeEach
    void setUp() {
        LocalStorageProperties properties = new LocalStorageProperties(
                tempDir.toString(), "http://localhost:8080", 900);
        provider = new LocalStorageProvider(properties);
    }

    @Test
    void issuedUploadUrlPointsAtTheLocalUploadEndpointWithTheRequiredContentTypeHeader() {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));

        assertThat(grant.uploadUrl()).startsWith("http://localhost:8080/local-storage/upload/");
        assertThat(grant.requiredHeaders()).containsEntry("Content-Type", "video/mp4");
    }

    @Test
    void receiveUploadWritesTheBytesAndMakesTheObjectFindable() {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));
        String token = tokenFromUrl(grant.uploadUrl());
        byte[] content = "fake mp4 bytes".getBytes(StandardCharsets.UTF_8);

        provider.receiveUpload(token, "video/mp4", content.length, bodyOf(content));

        Optional<StorageObjectMetadata> found = provider.find("videos/u1/a.mp4");
        assertThat(found).isPresent();
        assertThat(found.get().sizeBytes()).isEqualTo(content.length);
        assertThat(found.get().contentType()).isEqualTo("video/mp4");
    }

    @Test
    void receiveUploadIsSingleUseAndRejectsASecondAttemptWithTheSameToken() {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));
        String token = tokenFromUrl(grant.uploadUrl());
        byte[] content = "bytes".getBytes(StandardCharsets.UTF_8);
        provider.receiveUpload(token, "video/mp4", content.length, bodyOf(content));

        assertThatThrownBy(() -> provider.receiveUpload(token, "video/mp4", content.length, bodyOf(content)))
                .isInstanceOf(LocalUploadTokenException.class);
    }

    @Test
    void receiveUploadRejectsAContentTypeThatDoesNotMatchTheGrant() {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));
        String token = tokenFromUrl(grant.uploadUrl());
        byte[] content = "bytes".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> provider.receiveUpload(token, "video/quicktime", content.length, bodyOf(content)))
                .isInstanceOf(LocalUploadTokenException.class);

        assertThat(provider.find("videos/u1/a.mp4")).isEmpty();
    }

    @Test
    void receiveUploadRejectsContentDeclaredLargerThanTheGrantsCeiling() {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 10, Duration.ofMinutes(15));
        String token = tokenFromUrl(grant.uploadUrl());
        byte[] content = "this is way more than ten bytes".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> provider.receiveUpload(token, "video/mp4", content.length, bodyOf(content)))
                .isInstanceOf(LocalUploadTokenException.class);
    }

    @Test
    void receiveUploadWithAnUnknownTokenIsRejected() {
        assertThatThrownBy(() -> provider.receiveUpload("not-a-real-token", "video/mp4", 5, bodyOf("x".getBytes())))
                .isInstanceOf(LocalUploadTokenException.class);
    }

    @Test
    void deleteRemovesTheObjectSoItIsNoLongerFindable() {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));
        String token = tokenFromUrl(grant.uploadUrl());
        byte[] content = "bytes".getBytes(StandardCharsets.UTF_8);
        provider.receiveUpload(token, "video/mp4", content.length, bodyOf(content));

        provider.delete("videos/u1/a.mp4");

        assertThat(provider.find("videos/u1/a.mp4")).isEmpty();
    }

    @Test
    void findOnAKeyThatWasNeverUploadedIsEmpty() {
        assertThat(provider.find("videos/u1/does-not-exist.mp4")).isEmpty();
    }

    @Test
    void aKeyThatWouldEscapeTheStorageRootIsRejected() {
        assertThatThrownBy(() -> provider.find("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void issueDownloadUrlThenResolveDownloadReturnsTheUploadedFile() {
        UploadGrant uploadGrant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));
        String uploadToken = tokenFromUrl(uploadGrant.uploadUrl());
        byte[] content = "bytes".getBytes(StandardCharsets.UTF_8);
        provider.receiveUpload(uploadToken, "video/mp4", content.length, bodyOf(content));

        String downloadUrl = provider.issueDownloadUrl("videos/u1/a.mp4", Duration.ofMinutes(5));
        String downloadToken = tokenFromUrl(downloadUrl);

        Path resolved = provider.resolveDownload(downloadToken);
        assertThat(resolved.getFileName().toString()).isEqualTo("a.mp4");
    }

    private static InputStream bodyOf(byte[] content) {
        return new ByteArrayInputStream(content);
    }

    private static String tokenFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
