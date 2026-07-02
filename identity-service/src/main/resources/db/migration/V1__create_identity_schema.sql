CREATE TABLE identity_link (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_identity_link_provider_subject UNIQUE (provider, provider_subject)
);

CREATE INDEX idx_identity_link_customer_id ON identity_link (customer_id);
CREATE INDEX idx_identity_link_status ON identity_link (status);
