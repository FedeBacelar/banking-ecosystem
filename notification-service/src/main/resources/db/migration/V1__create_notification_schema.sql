CREATE TABLE notification (
    id CHAR(36) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    template_code VARCHAR(80) NOT NULL,
    variables_json JSON NOT NULL,
    correlation_id VARCHAR(120),
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    sent_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_notification_recipient ON notification (recipient);
CREATE INDEX idx_notification_template_code ON notification (template_code);
CREATE INDEX idx_notification_status ON notification (status);
CREATE INDEX idx_notification_correlation_id ON notification (correlation_id);
CREATE INDEX idx_notification_created_at ON notification (created_at);

