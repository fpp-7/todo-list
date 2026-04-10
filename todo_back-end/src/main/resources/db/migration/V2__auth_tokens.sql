CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_refresh_token_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_token_hash ON refresh_token(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_token_member_id ON refresh_token(member_id);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    CONSTRAINT fk_password_reset_token_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_hash ON password_reset_token(token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_member_id ON password_reset_token(member_id);
