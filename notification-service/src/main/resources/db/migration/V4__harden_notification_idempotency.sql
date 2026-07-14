ALTER TABLE notification
    ADD COLUMN request_fingerprint CHAR(64) NULL AFTER correlation_id;

UPDATE notification
SET variables_json = JSON_OBJECT(),
    body = '[REDACTED]',
    html_body = '[REDACTED]'
WHERE template_code IN (
    'ONBOARDING_EMAIL_MAGIC_LINK',
    'ONBOARDING_APPROVED_CREDENTIAL_INVITATION',
    'ONBOARDING_REJECTED',
    'ONBOARDING_COMPLETED'
);
