package com.visionselect.backend.player.dto;

import com.visionselect.backend.player.entity.BattingStyle;
import com.visionselect.backend.player.entity.BowlingStyle;
import com.visionselect.backend.player.entity.PlayerGender;
import com.visionselect.backend.player.entity.PlayerRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/players}.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = false)} rejects unknown JSON
 * fields, consistent with {@code VideoCreateRequest} and {@code RegisterRequest}.
 *
 * <p>Enum fields ({@code gender}, {@code battingStyle}, {@code bowlingStyle},
 * {@code primaryRole}) use Java enum types directly. Jackson deserialises the
 * string value from the request body into the enum, producing a 400 automatically
 * if the value is not a valid enum constant — no regex validation needed.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PlayerCreateRequest(

        @NotBlank
        @Size(max = 255)
        String fullName,

        /** ISO-8601 date string, e.g. {@code "2000-04-15"}. Nullable. */
        LocalDate dateOfBirth,

        /** Nullable — not required at profile creation. */
        PlayerGender gender,

        @Size(max = 100)
        String nationality,

        /** Nullable — not all players bat (e.g. specialist bowlers in rare cases). */
        BattingStyle battingStyle,

        /** Nullable — use {@code NOT_A_BOWLER} for pure batsmen if known. */
        BowlingStyle bowlingStyle,

        @NotNull
        PlayerRole primaryRole,

        @Min(0)
        @Max(999)
        Integer jerseyNumber,

        @Size(max = 255)
        String teamName,

        @Size(max = 1000)
        String profileImageKey
) {
}
