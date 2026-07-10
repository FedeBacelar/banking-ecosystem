ALTER TABLE identity_link
    ADD CONSTRAINT uk_identity_link_customer_provider UNIQUE (customer_id, provider);
