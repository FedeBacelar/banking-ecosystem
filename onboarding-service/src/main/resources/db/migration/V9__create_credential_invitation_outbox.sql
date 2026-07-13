CREATE TABLE onboarding_credential_invitation_delivery (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    idempotency_key_hash CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    locked_until TIMESTAMP(6) NULL,
    last_error_code VARCHAR(80) NULL,
    sent_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_credential_invitation_application_key
        UNIQUE (application_id, idempotency_key_hash),
    CONSTRAINT fk_credential_invitation_application
        FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);

CREATE INDEX idx_credential_invitation_due
    ON onboarding_credential_invitation_delivery (status, next_attempt_at);

CREATE INDEX idx_credential_invitation_application_created
    ON onboarding_credential_invitation_delivery (application_id, created_at);
