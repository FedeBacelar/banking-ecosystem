CREATE TABLE party (
    id CHAR(36) NOT NULL,
    party_type VARCHAR(40) NOT NULL,
    lifecycle_status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE natural_person (
    party_id CHAR(36) NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    middle_name VARCHAR(120) NULL,
    last_name VARCHAR(120) NOT NULL,
    birth_date DATE NOT NULL,
    nationality VARCHAR(2) NOT NULL,
    PRIMARY KEY (party_id),
    CONSTRAINT fk_natural_person_party FOREIGN KEY (party_id) REFERENCES party (id)
);

CREATE TABLE customer (
    id CHAR(36) NOT NULL,
    party_id CHAR(36) NOT NULL,
    customer_number VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    onboarding_date DATE NOT NULL,
    closed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_party UNIQUE (party_id),
    CONSTRAINT uk_customer_number UNIQUE (customer_number),
    CONSTRAINT fk_customer_party FOREIGN KEY (party_id) REFERENCES party (id)
);

CREATE TABLE identification_document (
    id CHAR(36) NOT NULL,
    party_id CHAR(36) NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    document_number VARCHAR(80) NOT NULL,
    issuing_country VARCHAR(2) NOT NULL,
    expiration_date DATE NULL,
    primary_document BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_identification_document UNIQUE (document_type, document_number, issuing_country),
    CONSTRAINT fk_identification_document_party FOREIGN KEY (party_id) REFERENCES party (id)
);

CREATE TABLE contact_point (
    id CHAR(36) NOT NULL,
    party_id CHAR(36) NOT NULL,
    contact_type VARCHAR(40) NOT NULL,
    contact_value VARCHAR(255) NOT NULL,
    verified BOOLEAN NOT NULL,
    primary_contact BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contact_point_party FOREIGN KEY (party_id) REFERENCES party (id)
);

CREATE TABLE address (
    id CHAR(36) NOT NULL,
    party_id CHAR(36) NOT NULL,
    address_type VARCHAR(40) NOT NULL,
    street VARCHAR(160) NOT NULL,
    street_number VARCHAR(40) NOT NULL,
    city VARCHAR(120) NOT NULL,
    province VARCHAR(120) NOT NULL,
    postal_code VARCHAR(30) NOT NULL,
    country VARCHAR(2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_address_party FOREIGN KEY (party_id) REFERENCES party (id)
);

CREATE TABLE kyc_profile (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    kyc_status VARCHAR(40) NOT NULL,
    last_review_at TIMESTAMP(6) NULL,
    next_review_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_kyc_profile_customer UNIQUE (customer_id),
    CONSTRAINT fk_kyc_profile_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE customer_status_history (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    previous_status VARCHAR(40) NULL,
    new_status VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_status_history_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);
