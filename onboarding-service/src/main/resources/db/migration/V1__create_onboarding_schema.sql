CREATE TABLE onboarding_application (
    id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    magic_link_token_hash CHAR(64) NOT NULL,
    magic_link_expires_at TIMESTAMP(6) NOT NULL,
    magic_link_consumed_at TIMESTAMP(6) NULL,
    email_verified_at TIMESTAMP(6) NULL,
    continuation_token_hash CHAR(64) NULL,
    continuation_expires_at TIMESTAMP(6) NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_onboarding_magic_link_token_hash ON onboarding_application (magic_link_token_hash);
CREATE UNIQUE INDEX uk_onboarding_continuation_token_hash ON onboarding_application (continuation_token_hash);
CREATE INDEX idx_onboarding_email_status ON onboarding_application (email, status);
CREATE INDEX idx_onboarding_status ON onboarding_application (status);
CREATE INDEX idx_onboarding_expires_at ON onboarding_application (expires_at);
