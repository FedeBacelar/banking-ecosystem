CREATE TABLE customer_idempotency (
    idempotency_key VARCHAR(160) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    resource_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (idempotency_key),
    CONSTRAINT fk_customer_idempotency_resource FOREIGN KEY (resource_id) REFERENCES customer (id)
);
