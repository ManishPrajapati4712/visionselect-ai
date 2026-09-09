-- ============================================================
-- VisionSelect AI — V3: players table
--
-- Creates the cricket player profile table and enforces the
-- FK from videos.player_id → players.id that V2 deliberately
-- deferred until this module existed.
--
-- Safety pre-condition (verified before running):
--   SELECT COUNT(*) FROM videos WHERE player_id IS NOT NULL;
--   → 0 rows. FK can be added directly without NOT VALID.
-- ============================================================

CREATE TABLE players (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name         VARCHAR(255)  NOT NULL,
    date_of_birth     DATE          NULL,
    gender            VARCHAR(10)   NULL,
    nationality       VARCHAR(100)  NULL,
    batting_style     VARCHAR(30)   NULL,
    bowling_style     VARCHAR(30)   NULL,
    primary_role      VARCHAR(20)   NOT NULL,
    jersey_number     SMALLINT      NULL,
    team_name         VARCHAR(255)  NULL,
    profile_image_key VARCHAR(1000) NULL,
    created_by        UUID          NOT NULL REFERENCES users (id),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ   NULL,

    CONSTRAINT chk_players_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),

    CONSTRAINT chk_players_batting_style
        CHECK (batting_style IN ('RIGHT_HANDED', 'LEFT_HANDED')),

    CONSTRAINT chk_players_bowling_style
        CHECK (bowling_style IN (
            'RIGHT_ARM_FAST', 'RIGHT_ARM_MEDIUM',
            'RIGHT_ARM_OFFBREAK', 'RIGHT_ARM_LEGBREAK',
            'LEFT_ARM_FAST', 'LEFT_ARM_MEDIUM',
            'LEFT_ARM_ORTHODOX', 'LEFT_ARM_WRIST_SPIN',
            'NOT_A_BOWLER'
        )),

    CONSTRAINT chk_players_primary_role
        CHECK (primary_role IN ('BATSMAN', 'BOWLER', 'ALL_ROUNDER', 'WICKET_KEEPER'))
);

-- Coach/admin query: "show me players I added"
CREATE INDEX ix_players_created_by ON players (created_by);

-- Role-based filtering for lists and future AI job queuing
CREATE INDEX ix_players_primary_role ON players (primary_role);

-- Efficient active-player queries — excludes soft-deleted rows at the index level
CREATE INDEX ix_players_active ON players (id) WHERE deleted_at IS NULL;

-- ── FK from videos.player_id → players.id ───────────────────────────────────
-- NOTE: The visionselect app user does not own the videos table (owned by postgres).
-- The FK constraint must be applied by the postgres superuser after this migration runs.
-- See the post-migration manual step documented in the implementation notes.
--
-- Command to run as superuser (postgres):
--   ALTER TABLE videos
--       ADD CONSTRAINT fk_videos_player_id
--       FOREIGN KEY (player_id)
--       REFERENCES players (id)
--       ON DELETE SET NULL;
--   GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON TABLE players TO visionselect;

