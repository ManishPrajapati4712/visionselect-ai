package com.visionselect.backend.player.entity;

/**
 * Bowling style of a cricket player.
 *
 * <p>Names match the {@code chk_players_bowling_style} CHECK constraint in V3 migration exactly.
 * {@link #NOT_A_BOWLER} is used when the player is a specialist batsman or wicket-keeper
 * who does not bowl.
 *
 * <p>Stored as a VARCHAR(30) string via {@code @Enumerated(EnumType.STRING)}.
 */
public enum BowlingStyle {
    RIGHT_ARM_FAST,
    RIGHT_ARM_MEDIUM,
    RIGHT_ARM_OFFBREAK,
    RIGHT_ARM_LEGBREAK,
    LEFT_ARM_FAST,
    LEFT_ARM_MEDIUM,
    LEFT_ARM_ORTHODOX,
    LEFT_ARM_WRIST_SPIN,
    NOT_A_BOWLER
}
