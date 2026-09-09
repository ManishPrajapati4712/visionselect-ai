package com.visionselect.backend.player.entity;

/**
 * Primary playing role for a cricket player.
 *
 * <p>Names match the {@code chk_players_primary_role} CHECK constraint in V3 migration exactly.
 * Stored as a VARCHAR(20) string via {@code @Enumerated(EnumType.STRING)}.
 */
public enum PlayerRole {
    BATSMAN,
    BOWLER,
    ALL_ROUNDER,
    WICKET_KEEPER
}
