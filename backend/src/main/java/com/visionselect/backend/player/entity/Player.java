package com.visionselect.backend.player.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Maps to the {@code players} table created by V3 Flyway migration.
 *
 * <p>Conventions follow existing entities ({@code User}, {@code Video}):
 * <ul>
 *   <li>UUID primary key via {@code @UuidGenerator}.</li>
 *   <li>No {@code @ManyToOne} — foreign keys held as raw {@code UUID} fields.</li>
 *   <li>Soft delete via {@code deleted_at}; {@code null} means active.</li>
 *   <li>{@code created_at} / {@code updated_at} managed by Hibernate annotations.</li>
 *   <li>Enum fields use {@code @Enumerated(EnumType.STRING)} so stored values match
 *       the PostgreSQL CHECK constraint names exactly.</li>
 * </ul>
 */
@Entity
@Table(name = "players")
public class Player {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** Player's date of birth. Maps to {@code DATE} column — stored without time component. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private PlayerGender gender;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "batting_style", length = 30)
    private BattingStyle battingStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "bowling_style", length = 30)
    private BowlingStyle bowlingStyle;

    /** Primary cricket role — mandatory for AI model routing. */
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_role", nullable = false, length = 20)
    private PlayerRole primaryRole;

    /** Jersey number (0–999). {@code null} if unknown. Maps to {@code SMALLINT}. */
    @Column(name = "jersey_number")
    private Short jerseyNumber;

    @Column(name = "team_name", length = 255)
    private String teamName;

    /**
     * Storage key for the player's profile image.
     * Follows the same opaque-key convention as {@code Video.storageKey}.
     * {@code null} until a profile image is uploaded.
     */
    @Column(name = "profile_image_key", length = 1000)
    private String profileImageKey;

    /**
     * UUID of the ADMIN or COACH who registered this player profile.
     * Raw UUID — no {@code @ManyToOne}, per project convention.
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Soft-delete marker. {@code null} means the profile is active. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Player() {
        // JPA no-args constructor
    }

    public Player(String fullName, LocalDate dateOfBirth, PlayerGender gender,
                  String nationality, BattingStyle battingStyle, BowlingStyle bowlingStyle,
                  PlayerRole primaryRole, Short jerseyNumber, String teamName,
                  String profileImageKey, UUID createdBy) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationality = nationality;
        this.battingStyle = battingStyle;
        this.bowlingStyle = bowlingStyle;
        this.primaryRole = primaryRole;
        this.jerseyNumber = jerseyNumber;
        this.teamName = teamName;
        this.profileImageKey = profileImageKey;
        this.createdBy = createdBy;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()                { return id; }
    public String getFullName()        { return fullName; }
    public LocalDate getDateOfBirth()  { return dateOfBirth; }
    public PlayerGender getGender()    { return gender; }
    public String getNationality()     { return nationality; }
    public BattingStyle getBattingStyle()  { return battingStyle; }
    public BowlingStyle getBowlingStyle()  { return bowlingStyle; }
    public PlayerRole getPrimaryRole() { return primaryRole; }
    public Short getJerseyNumber()     { return jerseyNumber; }
    public String getTeamName()        { return teamName; }
    public String getProfileImageKey() { return profileImageKey; }
    public UUID getCreatedBy()         { return createdBy; }
    public Instant getCreatedAt()      { return createdAt; }
    public Instant getUpdatedAt()      { return updatedAt; }
    /** Returns the soft-delete timestamp, or {@code null} if the profile is active. */
    public Instant getDeletedAt()      { return deletedAt; }

    // ── Mutation ─────────────────────────────────────────────────────────────

    /** Applies a soft delete by setting {@code deletedAt} to the current instant. */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    // Update setters — used by PlayerService.update() for non-null fields only.

    public void setFullName(String fullName)               { this.fullName = fullName; }
    public void setDateOfBirth(LocalDate dateOfBirth)      { this.dateOfBirth = dateOfBirth; }
    public void setGender(PlayerGender gender)             { this.gender = gender; }
    public void setNationality(String nationality)         { this.nationality = nationality; }
    public void setBattingStyle(BattingStyle battingStyle) { this.battingStyle = battingStyle; }
    public void setBowlingStyle(BowlingStyle bowlingStyle) { this.bowlingStyle = bowlingStyle; }
    public void setPrimaryRole(PlayerRole primaryRole)     { this.primaryRole = primaryRole; }
    public void setJerseyNumber(Short jerseyNumber)        { this.jerseyNumber = jerseyNumber; }
    public void setTeamName(String teamName)               { this.teamName = teamName; }
    public void setProfileImageKey(String profileImageKey) { this.profileImageKey = profileImageKey; }
}
