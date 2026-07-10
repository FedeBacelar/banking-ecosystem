ALTER TABLE onboarding_application
    ADD COLUMN review_mode VARCHAR(20) NOT NULL DEFAULT 'AUTO' AFTER status,
    ADD COLUMN review_policy_version VARCHAR(80) NOT NULL DEFAULT 'AR_DNI_SAVINGS_V1' AFTER review_mode,
    ADD COLUMN submitted_at TIMESTAMP(6) NULL AFTER expires_at,
    ADD COLUMN decided_at TIMESTAMP(6) NULL AFTER submitted_at;

CREATE TABLE onboarding_status_history (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    previous_status VARCHAR(40) NULL,
    new_status VARCHAR(40) NOT NULL,
    reason_code VARCHAR(80) NOT NULL,
    actor_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_onboarding_status_history_application FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);
CREATE INDEX idx_onboarding_status_history_application ON onboarding_status_history (application_id, occurred_at);

CREATE TABLE onboarding_review_check (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    check_type VARCHAR(50) NOT NULL,
    execution_mode VARCHAR(30) NOT NULL,
    execution_status VARCHAR(30) NOT NULL,
    outcome VARCHAR(40) NULL,
    blocking_check BOOLEAN NOT NULL,
    policy_version VARCHAR(80) NOT NULL,
    provider VARCHAR(80) NULL,
    reason_code VARCHAR(80) NULL,
    attempts INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_onboarding_review_check_application_type UNIQUE (application_id, check_type),
    CONSTRAINT fk_onboarding_review_check_application FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);
CREATE INDEX idx_onboarding_review_check_application ON onboarding_review_check (application_id);

CREATE TABLE onboarding_work_item (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    job_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    locked_until TIMESTAMP(6) NULL,
    last_error_code VARCHAR(80) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_onboarding_work_item_application_type UNIQUE (application_id, job_type),
    CONSTRAINT fk_onboarding_work_item_application FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);
CREATE INDEX idx_onboarding_work_item_due ON onboarding_work_item (job_type, status, next_attempt_at);
