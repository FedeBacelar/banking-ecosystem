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
