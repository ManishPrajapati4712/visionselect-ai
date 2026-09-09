package com.visionselect.backend.player.dto;

import com.visionselect.backend.player.entity.BattingStyle;
import com.visionselect.backend.player.entity.BowlingStyle;
import com.visionselect.backend.player.entity.Player;
import com.visionselect.backend.player.entity.PlayerGender;
import com.visionselect.backend.player.entity.PlayerRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a single player profile.
 *
 * <p>{@code deletedAt} is intentionally omitted — soft-deleted players are never
 * returned by the API (they produce 404), so exposing this field would be misleading.
 * This matches the pattern in {@code VideoResponse} which also omits {@code deletedAt}.
 *
 * <p>Enum fields are serialised as their string name by Jackson (e.g. {@code "BATSMAN"},
 * {@code "RIGHT_HANDED"}) because {@code JacksonConfig} disables
 * {@code WRITE_ENUMS_USING_TO_STRING} — the default {@code name()} is used.
 */
public record PlayerResponse(
        UUID id,
        String fullName,
        LocalDate dateOfBirth,
        PlayerGender gender,
        String nationality,
        BattingStyle battingStyle,
        BowlingStyle bowlingStyle,
        PlayerRole primaryRole,
        Integer jerseyNumber,
        String teamName,
        String profileImageKey,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getFullName(),
                player.getDateOfBirth(),
                player.getGender(),
                player.getNationality(),
                player.getBattingStyle(),
                player.getBowlingStyle(),
                player.getPrimaryRole(),
                player.getJerseyNumber() != null ? player.getJerseyNumber().intValue() : null,
                player.getTeamName(),
                player.getProfileImageKey(),
                player.getCreatedBy(),
                player.getCreatedAt(),
                player.getUpdatedAt()
        );
    }
}
