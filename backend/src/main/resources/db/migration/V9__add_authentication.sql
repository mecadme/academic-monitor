ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);

CREATE TABLE auth_refresh_tokens
(
    id                   UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id              UUID NOT NULL,
    institution_id       UUID NOT NULL,
    token_hash           VARCHAR(64) NOT NULL,
    family_id            UUID NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    expires_at           TIMESTAMPTZ NOT NULL,
    revoked_at           TIMESTAMPTZ,
    replaced_by_token_id  UUID,

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refresh_institution FOREIGN KEY (institution_id) REFERENCES institutions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refresh_membership FOREIGN KEY (user_id, institution_id)
        REFERENCES institution_memberships (user_id, institution_id) ON DELETE RESTRICT,
    CONSTRAINT fk_refresh_replacement FOREIGN KEY (replaced_by_token_id)
        REFERENCES auth_refresh_tokens (id) ON DELETE RESTRICT,
    CONSTRAINT chk_refresh_lifetime CHECK (expires_at > created_at),
    CONSTRAINT chk_refresh_replacement_revoked CHECK (replaced_by_token_id IS NULL OR revoked_at IS NOT NULL)
);

CREATE INDEX idx_refresh_user ON auth_refresh_tokens (user_id);
CREATE INDEX idx_refresh_institution ON auth_refresh_tokens (institution_id);
CREATE INDEX idx_refresh_family ON auth_refresh_tokens (family_id);
CREATE INDEX idx_refresh_expires ON auth_refresh_tokens (expires_at);
