ALTER TABLE customer_status_history
    ADD COLUMN changed_by VARCHAR(100) NOT NULL DEFAULT 'system' AFTER reason;
