-- ============================================================
-- VisionSelect AI — V4: AI Job Pipeline tables
-- ============================================================

CREATE TABLE ai_jobs (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id            UUID            NOT NULL REFERENCES videos (id),
    player_id           UUID            NOT NULL REFERENCES players (id),
    submitted_by        UUID            NOT NULL REFERENCES users (id),
    job_type            VARCHAR(30)     NOT NULL,
    provider            VARCHAR(50)     NOT NULL DEFAULT 'PYTHON_AI_SERVICE',
    status              VARCHAR(20)     NOT NULL DEFAULT 'QUEUED',
    progress_pct        SMALLINT        NULL,
    attempt_count       SMALLINT        NOT NULL DEFAULT 0,
    max_attempts        SMALLINT        NOT NULL DEFAULT 3,
    error_code          VARCHAR(100)    NULL,
    error_message       TEXT            NULL,
    idempotency_key     VARCHAR(255)    NOT NULL,
    queued_at           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    started_at          TIMESTAMPTZ     NULL,
    completed_at        TIMESTAMPTZ     NULL,
    failed_at           TIMESTAMPTZ     NULL,
    next_retry_at       TIMESTAMPTZ     NULL,
    callback_received_at TIMESTAMPTZ   NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_ai_jobs_status   CHECK (status   IN ('QUEUED','PROCESSING','COMPLETED','FAILED','RETRYING','CANCELLED')),
    CONSTRAINT chk_ai_jobs_job_type CHECK (job_type IN ('FULL_ANALYSIS','BATTING_ONLY','BOWLING_ONLY','FIELDING_ONLY')),
    CONSTRAINT chk_ai_jobs_provider CHECK (provider IN ('PYTHON_AI_SERVICE','MOCK')),
    CONSTRAINT chk_ai_jobs_progress CHECK (progress_pct IS NULL OR (progress_pct >= 0 AND progress_pct <= 100)),
    CONSTRAINT chk_ai_jobs_attempts CHECK (attempt_count >= 0 AND max_attempts >= 1 AND attempt_count <= max_attempts),
    CONSTRAINT uq_ai_jobs_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX ix_ai_jobs_player_id    ON ai_jobs (player_id);
CREATE INDEX ix_ai_jobs_video_id     ON ai_jobs (video_id);
CREATE INDEX ix_ai_jobs_submitted_by ON ai_jobs (submitted_by);
CREATE INDEX ix_ai_jobs_status_queue ON ai_jobs (status, queued_at) WHERE status IN ('QUEUED', 'RETRYING');
CREATE INDEX ix_ai_jobs_processing   ON ai_jobs (started_at) WHERE status = 'PROCESSING';
CREATE UNIQUE INDEX uq_ai_jobs_active_per_video_type ON ai_jobs (video_id, job_type) WHERE status IN ('QUEUED', 'PROCESSING', 'RETRYING');

CREATE TABLE ai_analysis_results (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    ai_job_id               UUID            NOT NULL UNIQUE REFERENCES ai_jobs (id) ON DELETE CASCADE,
    video_id                UUID            NOT NULL REFERENCES videos (id),
    player_id               UUID            NOT NULL REFERENCES players (id),
    overall_score           NUMERIC(5,2)    NULL,
    batting_score           NUMERIC(5,2)    NULL,
    bowling_score           NUMERIC(5,2)    NULL,
    fielding_score          NUMERIC(5,2)    NULL,
    fitness_score           NUMERIC(5,2)    NULL,
    confidence              NUMERIC(4,3)    NULL,
    raw_metrics             JSONB           NULL,
    feature_contributions   JSONB           NULL,
    explanation_text        JSONB           NULL,
    model_metadata          JSONB           NOT NULL DEFAULT '{}',
    human_reviewed          BOOLEAN         NOT NULL DEFAULT false,
    reviewed_by             UUID            NULL REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at             TIMESTAMPTZ     NULL,
    original_overall_score  NUMERIC(5,2)    NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_results_overall    CHECK (overall_score  IS NULL OR overall_score  BETWEEN 0 AND 100),
    CONSTRAINT chk_results_batting    CHECK (batting_score  IS NULL OR batting_score  BETWEEN 0 AND 100),
    CONSTRAINT chk_results_bowling    CHECK (bowling_score  IS NULL OR bowling_score  BETWEEN 0 AND 100),
    CONSTRAINT chk_results_fielding   CHECK (fielding_score IS NULL OR fielding_score BETWEEN 0 AND 100),
    CONSTRAINT chk_results_fitness    CHECK (fitness_score  IS NULL OR fitness_score  BETWEEN 0 AND 100),
    CONSTRAINT chk_results_confidence CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
);

CREATE INDEX ix_results_player_id   ON ai_analysis_results (player_id, created_at DESC);
CREATE INDEX ix_results_video_id    ON ai_analysis_results (video_id);
CREATE INDEX ix_results_feature_gin ON ai_analysis_results USING gin (feature_contributions);
