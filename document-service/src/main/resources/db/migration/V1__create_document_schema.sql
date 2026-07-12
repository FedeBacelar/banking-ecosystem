CREATE TABLE document (
    id CHAR(36) NOT NULL,
    business_context VARCHAR(80) NOT NULL,
    business_reference_id VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_provider VARCHAR(40) NOT NULL,
    bucket_name VARCHAR(120) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_document_object_key ON document (object_key);
CREATE INDEX idx_document_business_reference ON document (business_context, business_reference_id);
CREATE INDEX idx_document_category ON document (category);
CREATE INDEX idx_document_status ON document (status);
CREATE INDEX idx_document_created_at ON document (created_at);
