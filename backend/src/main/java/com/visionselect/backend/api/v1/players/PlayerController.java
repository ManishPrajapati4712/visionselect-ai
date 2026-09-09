package com.visionselect.backend.api.v1.players;

import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.response.ApiResponse;
import com.visionselect.backend.common.util.ApiPaths;
import com.visionselect.backend.player.dto.PlayerCreateRequest;
import com.visionselect.backend.player.dto.PlayerResponse;
import com.visionselect.backend.player.dto.PlayerUpdateRequest;
import com.visionselect.backend.player.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Players module.
 *
 * <p>RBAC (enforced via {@code @PreAuthorize} + {@code @EnableMethodSecurity}
 * in {@code SecurityConfig}):
 * <ul>
 *   <li>POST   — ADMIN, COACH</li>
 *   <li>GET list — ADMIN, COACH, SELECTOR</li>
 *   <li>GET one  — ADMIN, COACH, SELECTOR</li>
 *   <li>PUT      — ADMIN, COACH (ownership enforced in service)</li>
 *   <li>DELETE   — ADMIN, COACH (ownership enforced in service)</li>
 * </ul>
 *
 * <p>All responses use the standard {@link ApiResponse} envelope.
 * List results are paginated — default page size 20, sorted by {@code created_at} desc.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    /**
     * Create a new player profile.
     * ADMIN and COACH only.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<PlayerResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PlayerCreateRequest request) {
        return ApiResponse.success(playerService.create(principal, request), "Player created");
    }

    /**
     * List all active player profiles (paginated internally; returns content list).
     * ADMIN, COACH, and SELECTOR.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COACH','SELECTOR')")
    public ApiResponse<List<PlayerResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(playerService.list(pageable).getContent());
    }

    /**
     * Get a single active player by ID.
     * ADMIN, COACH, and SELECTOR.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COACH','SELECTOR')")
    public ApiResponse<PlayerResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(playerService.getById(id));
    }

    /**
     * Update a player profile (non-null fields only).
     * ADMIN can update any player; COACH can update only their own.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<PlayerResponse> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PlayerUpdateRequest request) {
        return ApiResponse.success(playerService.update(id, principal, request), "Player updated");
    }

    /**
     * Soft-delete a player profile.
     * ADMIN can delete any player; COACH can delete only their own.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        playerService.delete(id, principal);
        return ApiResponse.success(null, "Player deleted");
    }
}
