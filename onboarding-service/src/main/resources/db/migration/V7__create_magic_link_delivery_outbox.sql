CREATE TABLE onboarding_magic_link_delivery (
    application_id CHAR(36) NOT NULL,
    delivery_id CHAR(36) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    encrypted_magic_link TEXT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    sent_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (application_id),
    CONSTRAINT uk_magic_link_delivery_id UNIQUE (delivery_id),
    CONSTRAINT fk_magic_link_delivery_application
        FOREIGN KEY (application_id) REFERENCES onboarding_application (id)
);

CREATE INDEX idx_magic_link_delivery_pending
    ON onboarding_magic_link_delivery (sent_at, expires_at);
