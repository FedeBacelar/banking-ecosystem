# Configuration Repository

This directory contains externalized configuration served by `config-server`.

The current setup uses Spring Cloud Config Server in native mode, which reads these files directly from the local filesystem.

## Files

```txt
application.yaml
customer-service.yaml
account-service.yaml
identity-service.yaml
notification-service.yaml
document-service.yaml
onboarding-service.yaml
home-banking-bff.yaml
eureka-server.yaml
api-gateway.yaml
```

`application.yaml` contains shared configuration. Service-specific files contain configuration for each application name.

## Sensitive Values

Do not store real secrets in this directory.

Configuration files may reference environment variables such as:

```txt
CUSTOMER_DB_PASSWORD
ACCOUNT_DB_PASSWORD
ACCOUNT_INTERNAL_OAUTH_CLIENT_ID
ACCOUNT_INTERNAL_OAUTH_CLIENT_SECRET
IDENTITY_DB_PASSWORD
NOTIFICATION_DB_PASSWORD
NOTIFICATION_SMTP_USERNAME
NOTIFICATION_SMTP_PASSWORD
NOTIFICATION_SMTP_FROM
NOTIFICATION_SMTP_AUTH
NOTIFICATION_SMTP_STARTTLS
NOTIFICATION_SMTP_STARTTLS_REQUIRED
NOTIFICATION_SMTP_SSL
NOTIFICATION_SMTP_CONNECTION_TIMEOUT_MS
NOTIFICATION_SMTP_READ_TIMEOUT_MS
NOTIFICATION_SMTP_WRITE_TIMEOUT_MS
NOTIFICATION_MAGIC_LINK_ALLOWED_ORIGINS
NOTIFICATION_KEYCLOAK_ACTION_ALLOWED_ORIGINS
DOCUMENT_DB_PASSWORD
DOCUMENT_STORAGE_ENDPOINT
DOCUMENT_STORAGE_ACCESS_KEY
DOCUMENT_STORAGE_SECRET_KEY
DOCUMENT_STORAGE_BUCKET
ONBOARDING_DB_PASSWORD
ONBOARDING_FRONTEND_MAGIC_LINK_BASE_URL
EUREKA_SERVER_URL
SWAGGER_OAUTH_CLIENT_ID
HOME_BANKING_BFF_PORT
HOME_BANKING_BFF_OAUTH_CLIENT_ID
HOME_BANKING_BFF_OAUTH_CLIENT_SECRET
HOME_BANKING_BFF_OAUTH_REDIRECT_URI
ONBOARDING_BFF_INTERNAL_OAUTH_CLIENT_ID
ONBOARDING_BFF_INTERNAL_OAUTH_CLIENT_SECRET
HOME_BANKING_INTERNAL_OAUTH_CLIENT_ID
HOME_BANKING_INTERNAL_OAUTH_CLIENT_SECRET
ONBOARDING_INTERNAL_OAUTH_CLIENT_ID
ONBOARDING_INTERNAL_OAUTH_CLIENT_SECRET
HOME_BANKING_BFF_COOKIE_SECURE
HOME_BANKING_BFF_ONBOARDING_COOKIE_SECURE
```

Local development may use safe defaults, but real credentials must come from outside the repository.

Mailpit is the credential-free SMTP default for both `notification-service` and
Keycloak. Ignored local `.env` files are for safe development overrides and
must not be used as the normal storage location for a real SMTP account. A
deployed environment injects provider credentials from its runtime or secrets
manager.

The notification action-link settings are allowlists of complete origins, not
host fragments. Every entry must contain exactly scheme, host, and effective
port, without user information, path, query, fragment, or wildcard. Frontend
and identity origins are configured separately because they are different trust
boundaries. The template supplies the exact path and accepted URL shape.

Template metadata also owns its complete variable set and audit sensitivity.
Requests with undeclared variables are rejected, and callers cannot opt a
sensitive template out of persistence redaction. The legacy request flag may
add redaction for compatibility, but never remove the template requirement.

Changing the public onboarding origin is one atomic deployment change:
`ONBOARDING_FRONTEND_MAGIC_LINK_BASE_URL` and
`NOTIFICATION_MAGIC_LINK_ALLOWED_ORIGINS` must include the same origin before
traffic moves. The end-to-end onboarding check verifies that contract.

Authenticated SMTP must use either mandatory STARTTLS or implicit SSL.
Authentication, username, and password are enabled together; finite connection,
read, and write timeouts are mandatory. Invalid combinations fail at startup.
