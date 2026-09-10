package com.visionselect.backend.api.v1.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visionselect.backend.ai.dto.AiJobCreateRequest;
import com.visionselect.backend.ai.dto.AiJobResponse;
import com.visionselect.backend.ai.entity.AiJobStatus;
import com.visionselect.backend.ai.entity.AiJobType;
import com.visionselect.backend.ai.entity.AiProvider;
import com.visionselect.backend.ai.repository.AiAnalysisResultRepository;
import com.visionselect.backend.ai.service.AiJobService;
import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc controller tests for {@link AiJobController}.
 *
 * <p>Same approach as V3: no Spring context, service mocked, {@code @PreAuthorize}
 * verified via reflection (not executed in standalone mode).
 */
class AiJobControllerTest {

    private AiJobService aiJobService;
    private AiAnalysisResultRepository resultRepository;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private static final UUID JOB_ID    = UUID.randomUUID();
    private static final UUID VIDEO_ID  = UUID.randomUUID();
    private static final UUID PLAYER_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        aiJobService     = mock(AiJobService.class);
        resultRepository = mock(AiAnalysisResultRepository.class);
        AiJobController controller = new AiJobController(aiJobService, resultRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UserRole role) {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, role, UUID.randomUUID());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private AiJobResponse sampleJobResponse() {
        return new AiJobResponse(JOB_ID, VIDEO_ID, PLAYER_ID, USER_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE,
                AiJobStatus.QUEUED, null, (short) 0, (short) 3,
                null, "idem-key-123",
                Instant.now(), null, null, null, Instant.now(), Instant.now());
    }

    // ── POST /api/v1/ai/jobs ──────────────────────────────────────────────────

    @Test
    void submitJob_asAdmin_returns202() throws Exception {
        authenticateAs(UserRole.ADMIN);
        when(aiJobService.submit(any(), any())).thenReturn(sampleJobResponse());

        AiJobCreateRequest req = new AiJobCreateRequest(VIDEO_ID, PLAYER_ID,
                "FULL_ANALYSIS", "idem-key-123");

        mockMvc.perform(post("/api/v1/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
    }

    @Test
    void submitJob_missingVideoId_returns400() throws Exception {
        authenticateAs(UserRole.ADMIN);
        // Missing videoId — send null
        String body = "{\"playerId\":\"" + PLAYER_ID + "\",\"jobType\":\"FULL_ANALYSIS\",\"idempotencyKey\":\"k1\"}";
        mockMvc.perform(post("/api/v1/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitJob_invalidJobType_returns400() throws Exception {
        authenticateAs(UserRole.ADMIN);
        String body = "{\"videoId\":\"" + VIDEO_ID + "\",\"playerId\":\"" + PLAYER_ID
                + "\",\"jobType\":\"INVALID_TYPE\",\"idempotencyKey\":\"k2\"}";
        mockMvc.perform(post("/api/v1/ai/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // NOTE: Anonymous-user 401 tests are intentionally omitted for standalone MockMvc.
    // @PreAuthorize annotations are verified via reflection in the annotation-presence
    // tests below. The actual 401 enforcement is tested by the full Spring Security
    // filter chain which is not available in standalone MockMvc (same as V3 pattern).


    // ── GET /api/v1/ai/jobs/{id} ──────────────────────────────────────────────

    @Test
    void getStatus_existingJob_returns200() throws Exception {
        authenticateAs(UserRole.SELECTOR);
        when(aiJobService.getById(eq(JOB_ID), any())).thenReturn(sampleJobResponse());

        mockMvc.perform(get("/api/v1/ai/jobs/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(JOB_ID.toString()));
    }

    @Test
    void getStatus_notFound_returns404() throws Exception {
        authenticateAs(UserRole.ADMIN);
        when(aiJobService.getById(eq(JOB_ID), any()))
                .thenThrow(new AiJobService.AiJobNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/ai/jobs/" + JOB_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST /api/v1/ai/jobs/{id}/cancel ─────────────────────────────────────

    @Test
    void cancelJob_asAdmin_returns200() throws Exception {
        authenticateAs(UserRole.ADMIN);
        AiJobResponse cancelled = new AiJobResponse(JOB_ID, VIDEO_ID, PLAYER_ID, USER_ID,
                AiJobType.FULL_ANALYSIS, AiProvider.PYTHON_AI_SERVICE,
                AiJobStatus.CANCELLED, null, (short) 0, (short) 3,
                null, "idem-key", Instant.now(), null, null, null, Instant.now(), Instant.now());
        when(aiJobService.cancel(eq(JOB_ID), any())).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/ai/jobs/" + JOB_ID + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ── @PreAuthorize annotation presence (reflection checks) ─────────────────

    @Test
    void submitMethod_hasPreAuthorizeAnnotation() throws Exception {
        Method method = AiJobController.class.getMethod("submit", AuthenticatedUser.class, AiJobCreateRequest.class);
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ADMIN").contains("COACH");
    }

    @Test
    void getStatusMethod_hasPreAuthorizeAnnotation() throws Exception {
        Method method = AiJobController.class.getMethod("getStatus", UUID.class, AuthenticatedUser.class);
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("SELECTOR");
    }

    @Test
    void cancelMethod_hasPreAuthorizeAnnotation() throws Exception {
        Method method = AiJobController.class.getMethod("cancel", UUID.class, AuthenticatedUser.class);
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ADMIN").contains("COACH");
    }

    @Test
    void getResultMethod_hasPreAuthorizeAnnotation() throws Exception {
        Method method = AiJobController.class.getMethod("getResult", UUID.class);
        PreAuthorize ann = method.getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("SELECTOR");
    }
}
