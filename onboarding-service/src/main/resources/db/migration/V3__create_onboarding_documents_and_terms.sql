CREATE TABLE onboarding_document_reference (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    category VARCHAR(40) NOT NULL,
    document_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_onboarding_document_reference_application
        FOREIGN KEY (application_id) REFERENCES onboarding_application (id),
    CONSTRAINT uk_onboarding_document_reference_application_category
        UNIQUE (application_id, category)
);

CREATE INDEX idx_onboarding_document_reference_document
    ON onboarding_document_reference (document_id);

CREATE TABLE onboarding_terms_acceptance (
    application_id CHAR(36) NOT NULL,
    terms_version VARCHAR(80) NOT NULL,
    accepted_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (application_id),
    CONSTRAINT fk_onboarding_terms_acceptance_application
        FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);
