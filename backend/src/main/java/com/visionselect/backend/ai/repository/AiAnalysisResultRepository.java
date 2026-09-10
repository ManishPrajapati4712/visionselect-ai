package com.visionselect.backend.ai.repository;

import com.visionselect.backend.ai.entity.AiAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, UUID> {

    /** Latest result for a player (ordered newest first). */
    List<AiAnalysisResult> findByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    /** All results for a specific video. */
    List<AiAnalysisResult> findByVideoIdOrderByCreatedAtDesc(UUID videoId);

    /** Result produced by a specific job (one-to-one). */
    Optional<AiAnalysisResult> findByAiJobId(UUID aiJobId);

    /** Latest single result for a player. */
    Optional<AiAnalysisResult> findFirstByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    /** Latest single result for a video. */
    Optional<AiAnalysisResult> findFirstByVideoIdOrderByCreatedAtDesc(UUID videoId);
}
