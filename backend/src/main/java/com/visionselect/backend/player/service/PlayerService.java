package com.visionselect.backend.player.service;

import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.exception.ApiException;
import com.visionselect.backend.player.dto.PlayerCreateRequest;
import com.visionselect.backend.player.dto.PlayerResponse;
import com.visionselect.backend.player.dto.PlayerUpdateRequest;
import com.visionselect.backend.player.entity.Player;
import com.visionselect.backend.player.repository.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for the Players module.
 *
 * <p>RBAC rules (enforced here and at the controller level via {@code @PreAuthorize}):
 * <ul>
 *   <li><b>CREATE</b>: ADMIN, COACH</li>
 *   <li><b>LIST / GET</b>: ADMIN, COACH, SELECTOR</li>
 *   <li><b>UPDATE</b>: ADMIN (any); COACH (own profiles only, where
 *       {@code player.createdBy == principal.userId()})</li>
 *   <li><b>DELETE</b>: ADMIN (any); COACH (own profiles only)</li>
 *   <li>SELECTOR and PLAYER roles have no write access.</li>
 * </ul>
 *
 * <p>All deletes are soft — the {@code deleted_at} column is set to the current
 * instant; the row is never physically removed. Because the FK uses
 * {@code ON DELETE SET NULL}, a physical delete (if ever done by other means) would
 * null out {@code videos.player_id}; soft delete leaves video links intact.
 *
 * <p>Update semantics: only non-null fields in {@link PlayerUpdateRequest} are
 * applied. A {@code null} value means "leave unchanged" in this version.
 */
@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    /** Creates a new active player profile. Caller must be ADMIN or COACH. */
    @Transactional
    public PlayerResponse create(AuthenticatedUser principal, PlayerCreateRequest request) {
        Short jerseyNum = request.jerseyNumber() != null
                ? request.jerseyNumber().shortValue()
                : null;

        Player player = new Player(
                request.fullName(),
                request.dateOfBirth(),
                request.gender(),
                request.nationality(),
                request.battingStyle(),
                request.bowlingStyle(),
                request.primaryRole(),
                jerseyNum,
                request.teamName(),
                request.profileImageKey(),
                principal.userId()
        );
        player = playerRepository.save(player);
        return PlayerResponse.from(player);
    }

    // ── List ─────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of active (non-deleted) player profiles.
     * Caller must be ADMIN, COACH, or SELECTOR.
     */
    @Transactional(readOnly = true)
    public Page<PlayerResponse> list(Pageable pageable) {
        return playerRepository.findAllByDeletedAtIsNull(pageable)
                .map(PlayerResponse::from);
    }

    // ── Get ──────────────────────────────────────────────────────────────────

    /**
     * Returns a single active player. Throws {@link PlayerNotFoundException}
     * if not found or soft-deleted. Caller must be ADMIN, COACH, or SELECTOR.
     */
    @Transactional(readOnly = true)
    public PlayerResponse getById(UUID id) {
        Player player = playerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(PlayerNotFoundException::new);
        return PlayerResponse.from(player);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Updates an active player profile.
     *
     * <ul>
     *   <li>ADMIN: can update any active player.</li>
     *   <li>COACH: can update only players they created
     *       ({@code player.createdBy == principal.userId()}).</li>
     * </ul>
     *
     * Only non-null fields in the request are applied; null means "leave unchanged".
     */
    @Transactional
    public PlayerResponse update(UUID id, AuthenticatedUser principal, PlayerUpdateRequest request) {
        Player player = resolveForWrite(id, principal);

        if (request.fullName()        != null) player.setFullName(request.fullName());
        if (request.dateOfBirth()     != null) player.setDateOfBirth(request.dateOfBirth());
        if (request.gender()          != null) player.setGender(request.gender());
        if (request.nationality()     != null) player.setNationality(request.nationality());
        if (request.battingStyle()    != null) player.setBattingStyle(request.battingStyle());
        if (request.bowlingStyle()    != null) player.setBowlingStyle(request.bowlingStyle());
        if (request.primaryRole()     != null) player.setPrimaryRole(request.primaryRole());
        if (request.jerseyNumber()    != null) player.setJerseyNumber(request.jerseyNumber().shortValue());
        if (request.teamName()        != null) player.setTeamName(request.teamName());
        if (request.profileImageKey() != null) player.setProfileImageKey(request.profileImageKey());

        player = playerRepository.save(player);
        return PlayerResponse.from(player);
    }

    // ── Delete (soft) ────────────────────────────────────────────────────────

    /**
     * Soft-deletes an active player profile.
     *
     * <ul>
     *   <li>ADMIN: can delete any active player.</li>
     *   <li>COACH: can delete only players they created.</li>
     * </ul>
     *
     * The database row is never physically removed. {@code videos.player_id}
     * is left intact (FK ON DELETE SET NULL only fires on physical deletion).
     */
    @Transactional
    public void delete(UUID id, AuthenticatedUser principal) {
        Player player = resolveForWrite(id, principal);
        player.softDelete();
        playerRepository.save(player);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the player for a write operation, enforcing ownership rules:
     * <ul>
     *   <li>ADMIN → any active player.</li>
     *   <li>COACH → active player they own; throws {@link PlayerAccessDeniedException}
     *       if the player exists but was created by someone else.</li>
     * </ul>
     * Throws {@link PlayerNotFoundException} if the player does not exist or is deleted.
     */
    private Player resolveForWrite(UUID id, AuthenticatedUser principal) {
        if (principal.role() == UserRole.ADMIN) {
            return playerRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(PlayerNotFoundException::new);
        }
        // COACH: first check if player exists at all (to give 404 vs 403)
        playerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(PlayerNotFoundException::new);
        // Then check ownership
        return playerRepository.findByIdAndCreatedByAndDeletedAtIsNull(id, principal.userId())
                .orElseThrow(PlayerAccessDeniedException::new);
    }

    // ── Domain exceptions ────────────────────────────────────────────────────

    public static class PlayerNotFoundException extends ApiException {
        public PlayerNotFoundException() {
            super(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND", "Player not found");
        }
    }

    public static class PlayerAccessDeniedException extends ApiException {
        public PlayerAccessDeniedException() {
            super(HttpStatus.FORBIDDEN, "PLAYER_ACCESS_DENIED",
                    "You do not have permission to modify this player profile");
        }
    }

    public static class PlayerAlreadyDeletedException extends ApiException {
        public PlayerAlreadyDeletedException() {
            super(HttpStatus.CONFLICT, "PLAYER_ALREADY_DELETED",
                    "This player profile has already been deleted");
        }
    }
}
