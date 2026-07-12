CREATE TABLE onboarding_email_request_guard (
    email VARCHAR(255) NOT NULL,
    last_requested_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (email)
);
