package com.visionselect.backend.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Maps to the {@code ai_analysis_results} table created by the V4 Flyway migration.
 *
 * <p>This entity is append-only: new analysis creates a new row. Existing rows
 * are never deleted (no {@code deleted_at}). JSONB columns are mapped to
 * {@code Map<String, Object>} via Hibernate's JSON type.
 */
@Entity
@Table(name = "ai_analysis_results")
public class AiAnalysisResult {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** One-to-one with the job that produced this result (raw UUID, no @ManyToOne). */
    @Column(name = "ai_job_id", nullable = false, updatable = false, unique = true)
    private UUID aiJobId;

    @Column(name = "video_id", nullable = false, updatable = false)
    private UUID videoId;

    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "batting_score", precision = 5, scale = 2)
    private BigDecimal battingScore;

    @Column(name = "bowling_score", precision = 5, scale = 2)
    private BigDecimal bowlingScore;

    @Column(name = "fielding_score", precision = 5, scale = 2)
    private BigDecimal fieldingScore;

    @Column(name = "fitness_score", precision = 5, scale = 2)
    private BigDecimal fitnessScore;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    /** Verbatim model output before normalisation — never returned to users directly. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_metrics", columnDefinition = "jsonb")
    private Map<String, Object> rawMetrics;

    /** Per-skill feature weights: {"batting": {"front_foot_drive": 0.88, ...}, ...} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_contributions", columnDefinition = "jsonb")
    private Map<String, Object> featureContributions;

    /** Human-readable "why this score?" explanation per category. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "explanation_text", columnDefinition = "jsonb")
    private Map<String, Object> explanationText;

    /** Provider/model/version/prompt_hash for audit and reproducibility. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> modelMetadata;

    @Column(name = "human_reviewed", nullable = false)
    private boolean humanReviewed = false;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Original AI score preserved when a human override is applied. */
    @Column(name = "original_overall_score", precision = 5, scale = 2)
    private BigDecimal originalOverallScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiAnalysisResult() { }

    public AiAnalysisResult(UUID aiJobId, UUID videoId, UUID playerId,
                            BigDecimal overallScore, BigDecimal battingScore,
                            BigDecimal bowlingScore, BigDecimal fieldingScore,
                            BigDecimal fitnessScore, BigDecimal confidence,
                            Map<String, Object> rawMetrics,
                            Map<String, Object> featureContributions,
                            Map<String, Object> explanationText,
                            Map<String, Object> modelMetadata) {
        this.aiJobId = aiJobId;
        this.videoId = videoId;
        this.playerId = playerId;
        this.overallScore = overallScore;
        this.battingScore = battingScore;
        this.bowlingScore = bowlingScore;
        this.fieldingScore = fieldingScore;
        this.fitnessScore = fitnessScore;
        this.confidence = confidence;
        this.rawMetrics = rawMetrics;
        this.featureContributions = featureContributions;
        this.explanationText = explanationText;
        this.modelMetadata = modelMetadata != null ? modelMetadata : Map.of();
    }

    public UUID getId()                             { return id; }
    public UUID getAiJobId()                        { return aiJobId; }
    public UUID getVideoId()                        { return videoId; }
    public UUID getPlayerId()                       { return playerId; }
    public BigDecimal getOverallScore()             { return overallScore; }
    public BigDecimal getBattingScore()             { return battingScore; }
    public BigDecimal getBowlingScore()             { return bowlingScore; }
    public BigDecimal getFieldingScore()            { return fieldingScore; }
    public BigDecimal getFitnessScore()             { return fitnessScore; }
    public BigDecimal getConfidence()               { return confidence; }
    public Map<String, Object> getRawMetrics()      { return rawMetrics; }
    public Map<String, Object> getFeatureContributions() { return featureContributions; }
    public Map<String, Object> getExplanationText() { return explanationText; }
    public Map<String, Object> getModelMetadata()   { return modelMetadata; }
    public boolean isHumanReviewed()                { return humanReviewed; }
    public UUID getReviewedBy()                     { return reviewedBy; }
    public Instant getReviewedAt()                  { return reviewedAt; }
    public BigDecimal getOriginalOverallScore()     { return originalOverallScore; }
    public Instant getCreatedAt()                   { return createdAt; }
    public Instant getUpdatedAt()                   { return updatedAt; }

    public void applyHumanReview(BigDecimal newOverallScore, UUID reviewerId) {
        this.originalOverallScore = this.overallScore;
        this.overallScore = newOverallScore;
        this.humanReviewed = true;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }
}
