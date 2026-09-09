package com.visionselect.backend.player.service;

import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.player.dto.PlayerCreateRequest;
import com.visionselect.backend.player.dto.PlayerResponse;
import com.visionselect.backend.player.dto.PlayerUpdateRequest;
import com.visionselect.backend.player.entity.BattingStyle;
import com.visionselect.backend.player.entity.BowlingStyle;
import com.visionselect.backend.player.entity.Player;
import com.visionselect.backend.player.entity.PlayerGender;
import com.visionselect.backend.player.entity.PlayerRole;
import com.visionselect.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerServiceTest {

    private PlayerRepository playerRepository;
    private PlayerService playerService;

    private final UUID adminId = UUID.randomUUID();
    private final UUID coachId = UUID.randomUUID();
    private final UUID otherCoachId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        playerService = new PlayerService(playerRepository);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthenticatedUser adminPrincipal() {
        return new AuthenticatedUser(adminId, UserRole.ADMIN, UUID.randomUUID());
    }

    private AuthenticatedUser coachPrincipal() {
        return new AuthenticatedUser(coachId, UserRole.COACH, UUID.randomUUID());
    }

    private AuthenticatedUser otherCoachPrincipal() {
        return new AuthenticatedUser(otherCoachId, UserRole.COACH, UUID.randomUUID());
    }

    private PlayerCreateRequest createRequest() {
        return new PlayerCreateRequest(
                "Rohit Sharma", LocalDate.of(1987, 4, 30),
                PlayerGender.MALE, "India",
                BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_OFFBREAK,
                PlayerRole.BATSMAN, 45, "Mumbai Indians", null);
    }

    private Player savedPlayer(UUID createdBy) {
        Player p = new Player(
                "Rohit Sharma", LocalDate.of(1987, 4, 30),
                PlayerGender.MALE, "India",
                BattingStyle.RIGHT_HANDED, BowlingStyle.RIGHT_ARM_OFFBREAK,
                PlayerRole.BATSMAN, (short) 45, "Mumbai Indians", null,
                createdBy);
        return p;
    }

    // ── create() ─────────────────────────────────────────────────────────────

    @Test
    void createSavesPlayerWithCorrectFieldsAndReturnsResponse() {
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        PlayerResponse response = playerService.create(coachPrincipal(), createRequest());

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(captor.capture());
        Player saved = captor.getValue();

        assertThat(saved.getFullName()).isEqualTo("Rohit Sharma");
        assertThat(saved.getPrimaryRole()).isEqualTo(PlayerRole.BATSMAN);
        assertThat(saved.getCreatedBy()).isEqualTo(coachId);
        assertThat(saved.getDeletedAt()).isNull();
        assertThat(response.fullName()).isEqualTo("Rohit Sharma");
        assertThat(response.primaryRole()).isEqualTo(PlayerRole.BATSMAN);
        assertThat(response.createdBy()).isEqualTo(coachId);
    }

    @Test
    void createSetsCreatedByToThePrincipalUserId() {
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        playerService.create(adminPrincipal(), createRequest());

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(adminId);
    }

    // ── list() ────────────────────────────────────────────────────────────────

    @Test
    void listReturnsMappedPageOfActivePlayers() {
        Pageable pageable = PageRequest.of(0, 20);
        Player player = savedPlayer(coachId);
        when(playerRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(player)));

        Page<PlayerResponse> page = playerService.list(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).fullName()).isEqualTo("Rohit Sharma");
    }

    // ── getById() ─────────────────────────────────────────────────────────────

    @Test
    void getByIdReturnsResponseForActivePlayer() {
        Player player = savedPlayer(coachId);
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId))
                .thenReturn(Optional.of(player));

        PlayerResponse response = playerService.getById(playerId);

        assertThat(response.fullName()).isEqualTo("Rohit Sharma");
    }

    @Test
    void getByIdThrowsNotFoundWhenPlayerDoesNotExist() {
        when(playerRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.getById(UUID.randomUUID()))
                .isInstanceOf(PlayerService.PlayerNotFoundException.class);
    }

    @Test
    void getByIdThrowsNotFoundWhenPlayerIsSoftDeleted() {
        // soft-deleted player returns empty from the repo query (filtered by IS NULL)
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.getById(playerId))
                .isInstanceOf(PlayerService.PlayerNotFoundException.class);
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Test
    void updateAsAdminAppliesNonNullFieldsToAnyPlayer() {
        Player player = savedPlayer(coachId);
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        PlayerUpdateRequest req = new PlayerUpdateRequest(
                null, null, null, "Pakistan", null, null, null, null, "Lahore", null);

        PlayerResponse response = playerService.update(playerId, adminPrincipal(), req);

        assertThat(response.nationality()).isEqualTo("Pakistan");
        assertThat(response.teamName()).isEqualTo("Lahore");
        assertThat(response.fullName()).isEqualTo("Rohit Sharma"); // unchanged
    }

    @Test
    void updateAsCoachSucceedsForOwnPlayer() {
        Player player = savedPlayer(coachId);
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.findByIdAndCreatedByAndDeletedAtIsNull(playerId, coachId))
                .thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        PlayerUpdateRequest req = new PlayerUpdateRequest(
                "Updated Name", null, null, null, null, null, null, null, null, null);

        PlayerResponse response = playerService.update(playerId, coachPrincipal(), req);

        assertThat(response.fullName()).isEqualTo("Updated Name");
    }

    @Test
    void updateAsCoachThrowsAccessDeniedForAnotherCoachsPlayer() {
        Player player = savedPlayer(coachId); // owned by coachId
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.findByIdAndCreatedByAndDeletedAtIsNull(playerId, otherCoachId))
                .thenReturn(Optional.empty()); // other coach doesn't own it

        PlayerUpdateRequest req = new PlayerUpdateRequest(
                "Hack", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> playerService.update(playerId, otherCoachPrincipal(), req))
                .isInstanceOf(PlayerService.PlayerAccessDeniedException.class);

        verify(playerRepository, never()).save(any());
    }

    @Test
    void updateNonexistentPlayerThrowsNotFound() {
        when(playerRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        PlayerUpdateRequest req = new PlayerUpdateRequest(
                "X", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> playerService.update(UUID.randomUUID(), adminPrincipal(), req))
                .isInstanceOf(PlayerService.PlayerNotFoundException.class);
    }

    // ── delete() ─────────────────────────────────────────────────────────────

    @Test
    void deleteAsAdminSoftDeletesAnyPlayer() {
        Player player = savedPlayer(coachId);
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        playerService.delete(playerId, adminPrincipal());

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull(); // soft-deleted
    }

    @Test
    void deleteAsCoachSoftDeletesOwnPlayer() {
        Player player = savedPlayer(coachId);
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.findByIdAndCreatedByAndDeletedAtIsNull(playerId, coachId))
                .thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        playerService.delete(playerId, coachPrincipal());

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void deleteAsCoachThrowsAccessDeniedForAnotherCoachsPlayer() {
        Player player = savedPlayer(coachId);
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.findByIdAndCreatedByAndDeletedAtIsNull(playerId, otherCoachId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.delete(playerId, otherCoachPrincipal()))
                .isInstanceOf(PlayerService.PlayerAccessDeniedException.class);

        verify(playerRepository, never()).save(any());
    }

    @Test
    void deleteNonexistentPlayerThrowsNotFound() {
        when(playerRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.delete(UUID.randomUUID(), adminPrincipal()))
                .isInstanceOf(PlayerService.PlayerNotFoundException.class);

        verify(playerRepository, never()).save(any());
    }

    @Test
    void softDeleteDoesNotPhysicallyRemoveRow() {
        Player player = savedPlayer(coachId);
        when(playerRepository.findByIdAndDeletedAtIsNull(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        playerService.delete(playerId, adminPrincipal());

        // verify save was called (soft-delete), not deleteById
        verify(playerRepository).save(any(Player.class));
        verify(playerRepository, never()).deleteById(any());
        verify(playerRepository, never()).delete(any());
    }
}
