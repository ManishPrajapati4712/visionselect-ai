package com.visionselect.backend.player.dto;

import com.visionselect.backend.player.entity.BattingStyle;
import com.visionselect.backend.player.entity.BowlingStyle;
import com.visionselect.backend.player.entity.PlayerGender;
import com.visionselect.backend.player.entity.PlayerRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for {@code PUT /api/v1/players/{id}}.
 *
 * <p>All fields are nullable. The service applies <em>only non-null fields</em> to the
 * existing player — a {@code null} value in this request means "leave the current
 * value unchanged". This version does NOT support clearing an existing field back to
 * {@code null}; that semantic can be added in a later iteration if needed.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = false)} rejects unknown JSON fields,
 * consistent with all other request DTOs in the project.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PlayerUpdateRequest(

        @Size(max = 255)
        String fullName,

        LocalDate dateOfBirth,

        PlayerGender gender,

        @Size(max = 100)
        String nationality,

        BattingStyle battingStyle,

        BowlingStyle bowlingStyle,

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
