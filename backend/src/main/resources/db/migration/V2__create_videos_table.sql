-- Video-upload module: adds the `videos` table only, per data-dictionary.md.
--
-- `player_id` is nullable per the spec and intentionally has NO foreign key
-- constraint here: `players` is a separate, not-yet-built module (out of
-- scope for this phase, same reasoning V1 used for skipping every table
-- beyond `users`/`refresh_tokens`). The FK is added in the migration that
-- creates `players`, at which point this column starts being enforced;
-- until then it's an opaque UUID the application never dereferences.

CREATE TABLE videos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename          VARCHAR(500)   NOT NULL,
    storage_key       VARCHAR(1000)  NOT NULL,
    mime_type         VARCHAR(50)    NOT NULL,
    file_size_bytes   BIGINT         NOT NULL,
    -- Populated after AI preprocessing (a later phase); never client-supplied.
    duration_seconds  NUMERIC(8,2)   NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING_UPLOAD',
    uploaded_by       UUID           NOT NULL REFERENCES users (id),
    player_id         UUID           NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ    NULL,

    CONSTRAINT chk_videos_status CHECK (status IN ('PENDING_UPLOAD', 'UPLOADED', 'DELETED')),
    CONSTRAINT chk_videos_file_size_bytes CHECK (file_size_bytes > 0 AND file_size_bytes <= 524288000)
);

-- storage_key is unique per data-dictionary.md - one video row per object.
CREATE UNIQUE INDEX ux_videos_storage_key ON videos (storage_key);

-- "My uploads" list, per data-dictionary.md's explicit index note.
CREATE INDEX ix_videos_uploaded_by ON videos (uploaded_by);

-- Player analysis history, per data-dictionary.md's explicit index note.
CREATE INDEX ix_videos_player_id ON videos (player_id);
