package com.visionselect.backend.storage.local;

import com.visionselect.backend.storage.UploadGrant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@link LocalStorageUploadController} against a real {@link
 * LocalStorageProvider} (temp directory, no mocks) - this endpoint's whole
 * job is standing in for a real cloud provider's own HTTP surface, so a
 * mocked provider would just be testing that Mockito calls a mock, not
 * that a PUT actually lands bytes on disk.
 */
class LocalStorageUploadControllerTest {

    @TempDir
    Path tempDir;

    private LocalStorageProvider provider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalStorageProperties properties = new LocalStorageProperties(
                tempDir.toString(), "http://localhost:8080", 900);
        provider = new LocalStorageProvider(properties);
        LocalStorageUploadController controller = new LocalStorageUploadController(provider);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void putToAValidUploadTokenSucceedsAndTheObjectBecomesDownloadable() throws Exception {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));
        String uploadPath = grant.uploadUrl().substring(grant.uploadUrl().indexOf("/local-storage"));
        byte[] content = "fake mp4 bytes".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(put(uploadPath)
                        .contentType("video/mp4")
                        .content(content))
                .andExpect(status().isOk());

        String downloadUrl = provider.issueDownloadUrl("videos/u1/a.mp4", Duration.ofMinutes(5));
        String downloadPath = downloadUrl.substring(downloadUrl.indexOf("/local-storage"));

        mockMvc.perform(get(downloadPath))
                .andExpect(status().isOk());
    }

    @Test
    void putWithAMismatchedContentTypeIsRejected() throws Exception {
        UploadGrant grant = provider.issueUploadUrl("videos/u1/a.mp4", "video/mp4", 1024, Duration.ofMinutes(15));
        String uploadPath = grant.uploadUrl().substring(grant.uploadUrl().indexOf("/local-storage"));

        mockMvc.perform(put(uploadPath)
                        .contentType("video/quicktime")
                        .content("bytes".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isForbidden());
    }

    @Test
    void putWithAnUnknownTokenIsRejected() throws Exception {
        mockMvc.perform(put("/local-storage/upload/not-a-real-token")
                        .contentType("video/mp4")
                        .content("bytes".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getWithAnUnknownDownloadTokenIsRejected() throws Exception {
        mockMvc.perform(get("/local-storage/download/not-a-real-token"))
                .andExpect(status().isForbidden());
    }
}
