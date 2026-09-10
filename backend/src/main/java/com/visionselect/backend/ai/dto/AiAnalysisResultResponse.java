package com.visionselect.backend.ai.dto;

import com.visionselect.backend.ai.entity.AiAnalysisResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response body for analysis result endpoints.
 *
 * <p>Deliberately omits {@code rawMetrics} (verbatim model output that should
 * not be exposed to end users). All other fields including explainability are included.
 */
public record AiAnalysisResultResponse(
        UUID id,
        UUID aiJobId,
        UUID videoId,
        UUID playerId,
        BigDecimal overallScore,
        BigDecimal battingScore,
        BigDecimal bowlingScore,
        BigDecimal fieldingScore,
        BigDecimal fitnessScore,
        BigDecimal confidence,
        Map<String, Object> featureContributions,
        Map<String, Object> explanationText,
        Map<String, Object> modelMetadata,
        boolean humanReviewed,
        UUID reviewedBy,
        Instant reviewedAt,
        Instant createdAt
) {
    public static AiAnalysisResultResponse from(AiAnalysisResult r) {
        return new AiAnalysisResultResponse(
                r.getId(), r.getAiJobId(), r.getVideoId(), r.getPlayerId(),
                r.getOverallScore(), r.getBattingScore(), r.getBowlingScore(),
                r.getFieldingScore(), r.getFitnessScore(), r.getConfidence(),
                r.getFeatureContributions(), r.getExplanationText(), r.getModelMetadata(),
                r.isHumanReviewed(), r.getReviewedBy(), r.getReviewedAt(), r.getCreatedAt());
    }
}
