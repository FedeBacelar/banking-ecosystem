CREATE TABLE onboarding_provisioning_step (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    step_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    request_hash CHAR(64) NULL,
    external_reference VARCHAR(255) NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    last_error_code VARCHAR(80) NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_onboarding_provisioning_application_step UNIQUE (application_id, step_type),
    CONSTRAINT fk_onboarding_provisioning_application FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);
CREATE INDEX idx_onboarding_provisioning_due ON onboarding_provisioning_step (status, next_attempt_at);
