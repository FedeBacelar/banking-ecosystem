ALTER TABLE notification
    ADD CONSTRAINT uk_notification_template_correlation
        UNIQUE (template_code, correlation_id);
