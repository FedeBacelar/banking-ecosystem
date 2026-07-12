ALTER TABLE document
    ADD COLUMN idempotency_key VARCHAR(200) NULL AFTER id,
    ADD COLUMN content_sha256 CHAR(64) NULL AFTER idempotency_key;

CREATE UNIQUE INDEX uk_document_idempotency_key ON document (idempotency_key);
CREATE INDEX idx_document_content_sha256 ON document (content_sha256);
