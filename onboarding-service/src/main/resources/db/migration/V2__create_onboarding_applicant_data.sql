CREATE TABLE onboarding_applicant_data (
    application_id CHAR(36) NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    middle_name VARCHAR(120) NULL,
    last_name VARCHAR(120) NOT NULL,
    birth_date DATE NOT NULL,
    nationality VARCHAR(2) NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    document_number VARCHAR(80) NOT NULL,
    document_issuing_country VARCHAR(2) NOT NULL,
    document_expiration_date DATE NULL,
    phone_number VARCHAR(40) NOT NULL,
    street VARCHAR(160) NOT NULL,
    street_number VARCHAR(40) NOT NULL,
    city VARCHAR(120) NOT NULL,
    province VARCHAR(120) NOT NULL,
    postal_code VARCHAR(30) NOT NULL,
    country VARCHAR(2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (application_id),
    CONSTRAINT fk_onboarding_applicant_data_application
        FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);

CREATE INDEX idx_onboarding_applicant_document
    ON onboarding_applicant_data (document_type, document_number, document_issuing_country);
