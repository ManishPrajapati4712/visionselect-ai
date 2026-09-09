package com.visionselect.backend.api.v1.players;

import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.exception.GlobalExceptionHandler;
import com.visionselect.backend.player.dto.PlayerCreateRequest;
import com.visionselect.backend.player.dto.PlayerResponse;
import com.visionselect.backend.player.dto.PlayerUpdateRequest;
import com.visionselect.backend.player.entity.BattingStyle;
import com.visionselect.backend.player.entity.BowlingStyle;
import com.visionselect.backend.player.entity.PlayerGender;
import com.visionselect.backend.player.entity.PlayerRole;
import com.visionselect.backend.player.service.PlayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests for {@link PlayerController}.
 *
 * <p>Same approach as {@code VideoControllerTest}: no Spring context, no DB,
 * {@code PlayerService} mocked, {@link AuthenticationPrincipalArgumentResolver}
 * registered manually. {@code @PreAuthorize} cannot be exercised in standalone
 * mode (no AOP proxy), so annotation presence is verified via reflection.
 */
class PlayerControllerTest {

    private PlayerService playerService;
    private MockMvc mockMvc;

    private static final UUID PLAYER_ID   = UUID.randomUUID();
    private static final UUID CREATED_BY  = UUID.randomUUID();
    private static final Instant NOW      = Instant.parse("2026-09-08T12:00:00Z");

    @BeforeEach
    void setUp() {
        playerService = mock(PlayerService.class);
        PlayerController controller = new PlayerController(playerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void authenticateAs(UserRole role) {
        AuthenticatedUser principal = new AuthenticatedUser(CREATED_BY, role, UUID.randomUUID());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PlayerResponse sampleResponse() {
        return new PlayerResponse(
                PLAYER_ID, "Virat Singh", null, PlayerGender.MALE,
                "India", BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_MEDIUM,
                PlayerRole.BATSMAN, null, "Mumbai Indians", null,
                CREATED_BY, NOW, NOW);
    }

    private String validCreateBody() {
        return """
                {"fullName":"Virat Singh","primaryRole":"BATSMAN","nationality":"India",
                 "battingStyle":"RIGHT_HANDED","teamName":"Mumbai Indians"}
                """;
    }

    // ── POST /api/v1/players ──────────────────────────────────────────────────

    @Test
    void createPlayerWithValidBodyReturns201AndSuccessEnvelope() throws Exception {
        authenticateAs(UserRole.COACH);
        when(playerService.create(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/players")
                        .contentType("application/json")
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.fullName").value("Virat Singh"))
                .andExpect(jsonPath("$.data.primaryRole").value("BATSMAN"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void createPlayerWithMissingFullNameReturns400WithFieldError() throws Exception {
        authenticateAs(UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/players")
                        .contentType("application/json")
                        .content("{\"primaryRole\":\"BATSMAN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors[0].field").value("fullName"));

        verify(playerService, never()).create(any(), any());
    }

    @Test
    void createPlayerWithMissingPrimaryRoleReturns400() throws Exception {
        authenticateAs(UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/players")
                        .contentType("application/json")
                        .content("{\"fullName\":\"Test Player\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(playerService, never()).create(any(), any());
    }

    @Test
    void createPlayerWithInvalidEnumValueReturns400() throws Exception {
        authenticateAs(UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/players")
                        .contentType("application/json")
                        .content("{\"fullName\":\"X\",\"primaryRole\":\"UMPIRE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(playerService, never()).create(any(), any());
    }

    /**
     * Unknown-field rejection via {@code @JsonIgnoreProperties(ignoreUnknown=false)} requires
     * the full Spring context to activate the global {@code FAIL_ON_UNKNOWN_PROPERTIES} setting
     * from {@code JacksonConfig}. Standalone MockMvc does not apply that customizer.
     * This is the same documented limitation as {@code VideoControllerTest}. The annotation
     * is present on both DTOs; a full {@code @SpringBootTest} integration test would exercise this.
     */

    // ── GET /api/v1/players ───────────────────────────────────────────────────

    @Test
    void listPlayersAsAdminReturns200() throws Exception {
        authenticateAs(UserRole.ADMIN);
        when(playerService.list(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].fullName").value("Virat Singh"));
    }

    @Test
    void listPlayersAsSelectorReturns200() throws Exception {
        authenticateAs(UserRole.SELECTOR);
        when(playerService.list(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/v1/players/{id} ──────────────────────────────────────────────

    @Test
    void getByIdReturns200ForExistingPlayer() throws Exception {
        authenticateAs(UserRole.COACH);
        when(playerService.getById(PLAYER_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/players/" + PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PLAYER_ID.toString()));
    }

    @Test
    void getByIdReturns404WhenServiceThrowsNotFound() throws Exception {
        authenticateAs(UserRole.ADMIN);
        when(playerService.getById(any())).thenThrow(new PlayerService.PlayerNotFoundException());

        mockMvc.perform(get("/api/v1/players/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PLAYER_NOT_FOUND"));
    }

    // ── PUT /api/v1/players/{id} ──────────────────────────────────────────────

    @Test
    void updatePlayerAsAdminReturns200() throws Exception {
        authenticateAs(UserRole.ADMIN);
        when(playerService.update(eq(PLAYER_ID), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/players/" + PLAYER_ID)
                        .contentType("application/json")
                        .content("{\"teamName\":\"Delhi Capitals\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void updatePlayerAsCoachReturns200() throws Exception {
        authenticateAs(UserRole.COACH);
        when(playerService.update(eq(PLAYER_ID), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/players/" + PLAYER_ID)
                        .contentType("application/json")
                        .content("{\"teamName\":\"Delhi Capitals\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updatePlayerWhenCoachDoesNotOwnReturns403() throws Exception {
        authenticateAs(UserRole.COACH);
        when(playerService.update(eq(PLAYER_ID), any(), any()))
                .thenThrow(new PlayerService.PlayerAccessDeniedException());

        mockMvc.perform(put("/api/v1/players/" + PLAYER_ID)
                        .contentType("application/json")
                        .content("{\"teamName\":\"Delhi Capitals\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].code").value("PLAYER_ACCESS_DENIED"));
    }

    // ── DELETE /api/v1/players/{id} ───────────────────────────────────────────

    @Test
    void deletePlayerAsAdminReturns200() throws Exception {
        authenticateAs(UserRole.ADMIN);

        mockMvc.perform(delete("/api/v1/players/" + PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Player deleted"));

        verify(playerService).delete(eq(PLAYER_ID), any());
    }

    @Test
    void deletePlayerWhenCoachDoesNotOwnReturns403() throws Exception {
        authenticateAs(UserRole.COACH);
        org.mockito.Mockito.doThrow(new PlayerService.PlayerAccessDeniedException())
                .when(playerService).delete(eq(PLAYER_ID), any());

        mockMvc.perform(delete("/api/v1/players/" + PLAYER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].code").value("PLAYER_ACCESS_DENIED"));
    }

    @Test
    void deleteNonexistentPlayerReturns404() throws Exception {
        authenticateAs(UserRole.ADMIN);
        UUID missing = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new PlayerService.PlayerNotFoundException())
                .when(playerService).delete(eq(missing), any());

        mockMvc.perform(delete("/api/v1/players/" + missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].code").value("PLAYER_NOT_FOUND"));
    }

    // ── RBAC annotation reflection checks ────────────────────────────────────

    /**
     * Verifies that each endpoint has the correct {@code @PreAuthorize} annotation.
     * Because standalone MockMvc has no Spring Security AOP, this reflection check
     * is the only way to guard against annotation regression — same approach as
     * {@code VideoControllerTest#bothEndpointsAreAnnotatedToRequireAdminOrCoach()}.
     */
    @Test
    void allEndpointsHaveCorrectPreAuthorizeAnnotations() throws Exception {
        // POST — create
        assertPreAuthorize("create", AuthenticatedUser.class, PlayerCreateRequest.class,
                "hasAnyRole('ADMIN','COACH')");

        // GET list
        assertPreAuthorize("list", Pageable.class,
                "hasAnyRole('ADMIN','COACH','SELECTOR')");

        // GET one
        assertPreAuthorize("getById", UUID.class,
                "hasAnyRole('ADMIN','COACH','SELECTOR')");

        // PUT
        assertPreAuthorize("update", UUID.class, AuthenticatedUser.class, PlayerUpdateRequest.class,
                "hasAnyRole('ADMIN','COACH')");

        // DELETE
        assertPreAuthorize("delete", UUID.class, AuthenticatedUser.class,
                "hasAnyRole('ADMIN','COACH')");
    }

    private void assertPreAuthorize(String methodName, Object... paramTypesAndExpected) {
        // Last element is the expected annotation value string
        String expected = (String) paramTypesAndExpected[paramTypesAndExpected.length - 1];
        Class<?>[] paramTypes = new Class[paramTypesAndExpected.length - 1];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypes[i] = (Class<?>) paramTypesAndExpected[i];
        }
        try {
            Method m = PlayerController.class.getMethod(methodName, paramTypes);
            PreAuthorize annotation = m.getAnnotation(PreAuthorize.class);
            assertThat(annotation)
                    .as("@PreAuthorize missing on %s", methodName)
                    .isNotNull();
            assertThat(annotation.value())
                    .as("@PreAuthorize value on %s", methodName)
                    .isEqualTo(expected);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Method not found: " + methodName, e);
        }
    }
}
