package com.visionselect.backend.player.entity;

/**
 * Gender of a cricket player profile.
 *
 * <p>Names match the {@code chk_players_gender} CHECK constraint in V3 migration exactly.
 * Stored as a VARCHAR(10) string via {@code @Enumerated(EnumType.STRING)}.
 */
public enum PlayerGender {
    MALE,
    FEMALE,
    OTHER
}
