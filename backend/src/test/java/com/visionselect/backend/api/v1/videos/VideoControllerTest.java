package com.visionselect.backend.api.v1.videos;

import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.exception.GlobalExceptionHandler;
import com.visionselect.backend.video.dto.UploadUrlResponse;
import com.visionselect.backend.video.dto.VideoResponse;
import com.visionselect.backend.video.exception.VideoObjectNotFoundException;
import com.visionselect.backend.video.service.VideoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc (no Spring context/DB), same pattern as {@code
 * UserControllerTest}: {@link AuthenticationPrincipalArgumentResolver}
 * registered manually so {@code @AuthenticationPrincipal} resolves.
 *
 * <p>One thing this style of test structurally cannot exercise: {@code
 * @PreAuthorize} is enforced by a method-security AOP proxy that only
 * exists inside a real Spring context ({@code @EnableMethodSecurity}) -
 * a bare {@code new VideoController(...)} has no such proxy, so every
 * request here reaches the controller method regardless of role. {@link
 * #bothEndpointsAreAnnotatedToRequireAdminOrCoach()} closes that gap with
 * a reflection check that the annotation - and its exact role list - is
 * actually present, so the RBAC intent from security-contract.md
 * ("Videos — upload/create": ADMIN yes, COACH yes, SELECTOR no, PLAYER no)
 * doesn't silently regress even though this test class can't drive it
 * end-to-end without the full security context.
 */
class VideoControllerTest {

    private VideoService videoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        videoService = mock(VideoService.class);
        VideoController controller = new VideoController(videoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestUploadUrlWithAValidBodyReturns200AndTheStandardSuccessEnvelope() throws Exception {
        authenticateAs(UserRole.COACH);
        UploadUrlResponse response = UploadUrlResponse.of(
                "videos/some-user/some-uuid.mp4",
                "http://localhost:8080/local-storage/upload/some-token",
                Instant.parse("2026-08-15T12:00:00Z"),
                Map.of("Content-Type", "video/mp4"));
        when(videoService.requestUploadUrl(any(), any())).thenReturn(response);

        String body = """
                {"filename":"match-footage.mp4","mimeType":"video/mp4","fileSizeBytes":1048576}
                """;

        mockMvc.perform(post("/api/v1/videos/upload-url")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.storageKey").value("videos/some-user/some-uuid.mp4"))
                .andExpect(jsonPath("$.data.method").value("PUT"))
                .andExpect(jsonPath("$.data.requiredHeaders['Content-Type']").value("video/mp4"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void requestUploadUrlWithMissingRequiredFieldsReturns400WithFieldLevelErrors() throws Exception {
        authenticateAs(UserRole.COACH);

        String body = "{}";

        mockMvc.perform(post("/api/v1/videos/upload-url")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(videoService, never()).requestUploadUrl(any(), any());
    }

    @Test
    void requestUploadUrlWithAFileSizeOverTheCapReturns400() throws Exception {
        authenticateAs(UserRole.COACH);

        String body = """
                {"filename":"huge.mp4","mimeType":"video/mp4","fileSizeBytes":600000000}
                """;

        mockMvc.perform(post("/api/v1/videos/upload-url")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fileSizeBytes"));

        verify(videoService, never()).requestUploadUrl(any(), any());
    }

    /**
     * {@code storageKey} and {@code status} are backend-generated/backend-owned
     * (see {@code UploadUrlRequest} javadoc) - same rejection posture as
     * {@code RegisterRequest} rejecting a client-supplied {@code role}.
     */
    @Test
    void requestUploadUrlContainingAClientSuppliedStorageKeyIsRejectedAndNeverReachesTheService() throws Exception {
        authenticateAs(UserRole.COACH);

        String body = """
                {"filename":"a.mp4","mimeType":"video/mp4","fileSizeBytes":1024,"storageKey":"videos/attacker/x.mp4"}
                """;

        mockMvc.perform(post("/api/v1/videos/upload-url")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(videoService, never()).requestUploadUrl(any(), any());
    }

    @Test
    void registerUploadWithAValidBodyReturns201AndTheStandardSuccessEnvelope() throws Exception {
        authenticateAs(UserRole.ADMIN);
        UUID videoId = UUID.randomUUID();
        UUID uploadedBy = UUID.randomUUID();
        VideoResponse response = new VideoResponse(
                videoId, "match-footage.mp4", 1_048_576L, "video/mp4",
                "videos/some-user/some-uuid.mp4", null, "UPLOADED", uploadedBy, null,
                Instant.parse("2026-08-15T12:00:00Z"));
        when(videoService.registerUpload(any(), any())).thenReturn(response);

        String body = """
                {"storageKey":"videos/some-user/some-uuid.mp4","filename":"match-footage.mp4","mimeType":"video/mp4","fileSizeBytes":1048576}
                """;

        mockMvc.perform(post("/api/v1/videos")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(videoId.toString()))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    /**
     * {@code status} is backend-assigned (see {@code VideoCreateRequest}
     * javadoc) - same rejection posture as the upload-url test above, and
     * as {@code RegisterRequest} rejecting a client-supplied {@code role}.
     */
    @Test
    void registerUploadContainingAClientSuppliedStatusIsRejectedAndNeverReachesTheService() throws Exception {
        authenticateAs(UserRole.ADMIN);

        String body = """
                {"storageKey":"videos/u1/a.mp4","filename":"a.mp4","mimeType":"video/mp4","fileSizeBytes":1024,"status":"UPLOADED"}
                """;

        mockMvc.perform(post("/api/v1/videos")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(videoService, never()).registerUpload(any(), any());
    }

    @Test
    void registerUploadWithAMissingStorageKeyReturns400() throws Exception {
        authenticateAs(UserRole.ADMIN);

        String body = """
                {"filename":"match-footage.mp4","mimeType":"video/mp4","fileSizeBytes":1048576}
                """;

        mockMvc.perform(post("/api/v1/videos")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("storageKey"));

        verify(videoService, never()).registerUpload(any(), any());
    }

    @Test
    void registerUploadPropagatesA404WhenTheServiceReportsTheObjectWasNeverUploaded() throws Exception {
        authenticateAs(UserRole.COACH);
        when(videoService.registerUpload(any(), any())).thenThrow(new VideoObjectNotFoundException());

        String body = """
                {"storageKey":"videos/some-user/never-uploaded.mp4","filename":"x.mp4","mimeType":"video/mp4","fileSizeBytes":1024}
                """;

        mockMvc.perform(post("/api/v1/videos")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("STORAGE_OBJECT_NOT_FOUND"));
    }

    /** See class javadoc for why this is reflection-based rather than an end-to-end 403 test. */
    @Test
    void bothEndpointsAreAnnotatedToRequireAdminOrCoach() throws Exception {
        Method uploadUrlMethod = VideoController.class.getMethod(
                "requestUploadUrl", AuthenticatedUser.class,
                com.visionselect.backend.video.dto.UploadUrlRequest.class);
        Method registerMethod = VideoController.class.getMethod(
                "registerUpload", AuthenticatedUser.class,
                com.visionselect.backend.video.dto.VideoCreateRequest.class);

        for (Method method : List.of(uploadUrlMethod, registerMethod)) {
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation).as("@PreAuthorize on %s", method.getName()).isNotNull();
            assertThat(annotation.value()).isEqualTo("hasAnyRole('ADMIN','COACH')");
        }
    }

    private void authenticateAs(UserRole role) {
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), role, UUID.randomUUID());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
