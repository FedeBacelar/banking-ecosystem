CREATE TABLE onboarding_uniqueness_reservation (
    id CHAR(36) NOT NULL,
    reservation_type VARCHAR(30) NOT NULL,
    normalized_value VARCHAR(255) NOT NULL,
    application_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_onboarding_reservation_type_value UNIQUE (reservation_type, normalized_value),
    CONSTRAINT fk_onboarding_reservation_application FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);
CREATE INDEX idx_onboarding_reservation_application ON onboarding_uniqueness_reservation (application_id, status);
