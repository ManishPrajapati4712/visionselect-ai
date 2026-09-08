-- VisionSelect AI — V1: users and refresh_tokens tables.
--
-- THIS FILE WAS CORRECTED to match the live database schema (visionselect)
-- as verified on 2026-09-07. The original file that executed on 2026-08-16
-- used full_name / email VARCHAR(255) / issued_at / replaced_by_token_id,
-- which is what this file now reflects.
--
-- The previous untracked version of this file used display_name / VARCHAR(320)
-- / created_at and was NEVER executed against the database.
--
-- After correction the Flyway checksum is reconciled via FlywayRepairConfig
-- (see src/main/java/.../config/FlywayRepairConfig.java) on first startup.
-- That repair call is SAFE: it updates the stored CRC32 in flyway_schema_history
-- without re-running the migration or changing the database schema.
--
-- Design notes:
--   users.role:          stored as a plain VARCHAR with a CHECK constraint;
--                        the application-level enum (UserRole) must stay in
--                        sync with these four values.
--   users.deleted_at:    soft-delete marker; NULL means the account is active.
--   refresh_tokens:      only the SHA-256 hex digest of the raw token is
--                        stored — never a plaintext token value.
--                        revoked_at is set by logout and token rotation.
--                        replaced_by_token_id supports rotation audit trail:
--                        the old token points to the replacement token.
--                        ON DELETE CASCADE keeps orphaned rows from
--                        accumulating if a user is ever hard-deleted.

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ  NULL,

    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'COACH', 'SELECTOR', 'PLAYER'))
);

-- Unique email lookup index.
CREATE UNIQUE INDEX ux_users_email ON users (email);

-- ---------------------------------------------------------------------------

CREATE TABLE refresh_tokens (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash           VARCHAR(255) NOT NULL UNIQUE,   -- SHA-256 hex digest
    issued_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at           TIMESTAMPTZ NOT NULL,
    revoked_at           TIMESTAMPTZ NULL,               -- set on logout / rotation
    replaced_by_token_id UUID        NULL
        REFERENCES refresh_tokens (id) ON DELETE SET NULL  -- rotation audit: old → new
);

-- Lookup by hash on every /refresh call.
CREATE INDEX ix_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- Lookup by user on logout (revokeAllByUserId).
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
