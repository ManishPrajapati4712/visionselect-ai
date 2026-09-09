package com.visionselect.backend.player.repository;

import com.visionselect.backend.player.entity.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link Player}.
 *
 * <p>All query methods filter by {@code deletedAt IS NULL} at the database
 * level so soft-deleted profiles never surface in normal application flows.
 */
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    /** Find an active player by ID. Returns empty if not found or soft-deleted. */
    Optional<Player> findByIdAndDeletedAtIsNull(UUID id);

    /** Paginated list of all active players. */
    Page<Player> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * Find an active player by ID that was created by a specific user.
     * Used by {@code PlayerService} to enforce COACH ownership on update/delete.
     */
    Optional<Player> findByIdAndCreatedByAndDeletedAtIsNull(UUID id, UUID createdBy);
}
