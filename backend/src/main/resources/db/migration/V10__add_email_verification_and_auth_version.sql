ALTER TABLE users
    ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN legacy_email_verification_exempt BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users
SET legacy_email_verification_exempt = TRUE
WHERE email_verified_at IS NULL;

CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    sent_to_email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_tokens_token_hash ON email_verification_tokens(token_hash);
CREATE INDEX idx_email_verification_tokens_expires_at ON email_verification_tokens(expires_at);
CREATE UNIQUE INDEX uq_email_verification_tokens_active_user
    ON email_verification_tokens(user_id)
    WHERE used_at IS NULL;
