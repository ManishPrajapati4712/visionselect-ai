package com.visionselect.backend.player.entity;

/**
 * Batting handedness of a cricket player.
 *
 * <p>Names match the {@code chk_players_batting_style} CHECK constraint in V3 migration exactly.
 * Stored as a VARCHAR(30) string via {@code @Enumerated(EnumType.STRING)}.
 */
public enum BattingStyle {
    RIGHT_HANDED,
    LEFT_HANDED
}
