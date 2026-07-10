# Keycloak Implementation

Keycloak is the current local identity provider infrastructure.

It is a platform component. It does not own banking business data.

## Current Status

Implemented as local infrastructure.

Current capabilities:

- Runs Keycloak in development mode.
- Exposes the Keycloak admin console locally.
- Persists local Keycloak data in a Docker volume.
- Supports local override of port and admin credentials through environment variables.
- Imports the `banking-ecosystem` realm on first startup.
- Provides a local `banking-api` client for API token testing.
- Provides a local `banking-swagger` client for service Swagger UI OAuth2 login with PKCE.
- Provides a local `home-banking-bff` confidential client for browser login through the BFF.
- Allows `home-banking-bff` to use client credentials for internal onboarding calls.
- Provides API roles for customer, account, identity, notification, document, and onboarding access.
- Issues JWT access tokens validated by `api-gateway`, `customer-service`, `account-service`, `identity-service`, `notification-service`, `document-service`, and `onboarding-service`.
- Provides a local `banking` login theme for the browser-facing authentication flow.

## Local Runtime

Default HTTP port:

```txt
8090
```

Admin console:

```txt
http://localhost:8090
```

## Public Access Model

Keycloak uses a dedicated auth endpoint.

It is not exposed through `api-gateway` as a business route.

Local development:

```txt
Gateway:  http://localhost:8085
Keycloak: http://localhost:8090
```

Production-style model:

```txt
API:  https://api.bank.example
Auth: https://auth.bank.example
```

## Configuration

Docker Compose file:

```txt
infra/keycloak/docker-compose.yml
```

Local environment example:

```txt
infra/keycloak/.env.example
```

Realm import:

```txt
infra/keycloak/realms/banking-ecosystem-realm.json
```

Login theme:

```txt
infra/keycloak/themes/banking/login
```

## Start

```powershell
docker compose -f infra/keycloak/docker-compose.yml up -d
```

## Stop

```powershell
docker compose -f infra/keycloak/docker-compose.yml down
```

## Current Realm

```txt
banking-ecosystem
```

Current login theme:

```txt
banking
```

Current clients:

```txt
banking-api
banking-swagger
home-banking-bff
onboarding-orchestrator
```

Current roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
IDENTITY_READ
IDENTITY_WRITE
NOTIFICATION_WRITE
DOCUMENT_READ
DOCUMENT_WRITE
ONBOARDING_READ
ONBOARDING_WRITE
```

Current local test users:

```txt
banking-admin
home-banking-user
```

Local user purpose:

```txt
banking-admin     -> local operational/API testing across current services
home-banking-user -> browser login through home-banking-bff
```

`banking-admin` is an operational user. It should not be used as the final customer in the BFF flow.

`home-banking-user` represents the typical home banking customer. The customer does not choose which `customerId` to read; the BFF resolves it from the Keycloak subject through `identity-service`.

## Service Integration

Current protected components:

```txt
api-gateway
customer-service
account-service
identity-service
notification-service
document-service
onboarding-service
```

All protected components validate JWT access tokens issued by the `banking-ecosystem` realm.

Role usage:

```txt
CUSTOMER_READ  -> read customer API
CUSTOMER_WRITE -> write customer API
ACCOUNT_READ   -> read account API
ACCOUNT_WRITE  -> write account API
IDENTITY_READ  -> read/resolve identity links
IDENTITY_WRITE -> create/update identity links
NOTIFICATION_WRITE -> send internal notifications
DOCUMENT_READ -> read document metadata
DOCUMENT_WRITE -> upload documents
ONBOARDING_READ -> read onboarding application metadata and validate continuations
ONBOARDING_WRITE -> start onboarding applications and consume magic links
```

The current role model is coarse-grained. The BFF enforces the customer-facing "own data" flow by deriving the `customerId` from the authenticated identity link and not accepting a customer id from the browser.

Future hardening should add ownership checks inside business services too, so direct API access cannot use a customer token to read another customer's data.

The imported realm intentionally keeps one operational/API admin user and one browser customer user. Extra users for negative authorization tests should be created temporarily when needed.

## Swagger Client

`banking-swagger` is used by service Swagger UIs.

It is a public client configured for Authorization Code with PKCE.

Local redirect URLs:

```txt
http://localhost:8080/swagger-ui/oauth2-redirect.html
http://localhost:8081/swagger-ui/oauth2-redirect.html
http://localhost:8082/swagger-ui/oauth2-redirect.html
http://localhost:8083/swagger-ui/oauth2-redirect.html
http://localhost:8084/swagger-ui/oauth2-redirect.html
http://localhost:8087/swagger-ui/oauth2-redirect.html
```

If a local Keycloak volume already existed before this client was added, add the client manually or recreate the local Keycloak volume.

## BFF Client

`home-banking-bff` is used by the browser-facing backend.

It is a confidential client configured for Authorization Code Flow.

Local redirect URLs:

```txt
http://localhost:8085/web/login/oauth2/code/keycloak
```

Local post logout redirect URLs:

```txt
http://localhost:8085/web/session
```

The browser-facing local flow must use the gateway URL on port `8085`. Direct BFF URLs on port `8086` are only for diagnostics and should not be registered as the normal browser callback.

The local secret in the realm import is only a development default. Real secrets must come from environment variables or a secrets manager.

The same confidential BFF client has service accounts enabled for backend-to-backend onboarding calls.

Current service account roles:

```txt
DOCUMENT_READ
DOCUMENT_WRITE
NOTIFICATION_WRITE
ONBOARDING_READ
ONBOARDING_WRITE
```

`NOTIFICATION_WRITE` is needed because the current `onboarding-service` magic-link flow requests email delivery through `notification-service`.
`DOCUMENT_WRITE` is needed because the current BFF applicant-data flow uploads DNI evidence through `document-service`.

This is not a human test user and should not be used for browser login.

## Onboarding Orchestrator Client

`onboarding-orchestrator` is a dedicated client-credentials client used only by `onboarding-service`. It has the minimum customer, account, identity, document, notification, and Keycloak user-administration roles needed by the process manager. It is not a shared human user and is never used by the browser.

## Credential Setup

Approved applicants are created with provisional username `pending-{applicationId}`, verified email, customer realm roles, and required actions `UPDATE_PROFILE` and `UPDATE_PASSWORD`.

Keycloak sends the action link itself through `execute-actions-email`; identity tokens are not transported through `notification-service`. SMTP values and the orchestrator secret are environment placeholders. Local values live in ignored `infra/keycloak/.env`; safe names live in `.env.example`.

The one-shot `keycloak-realm-init` container applies `banking-user-profile.json` after realm startup. The profile permits the applicant to choose the username while email, first name, and last name remain visible but user read-only. Unmanaged attributes remain on Keycloak's strict `DISABLED` default; the managed profile does not enable arbitrary user attributes.

The credential action lifespan defaults to 24 hours and redirects to:

```txt
http://localhost:4200/onboarding/credentials-complete
```

When `keycloak-realm-init` exits with code `0`, that stopped container is expected; it is an initialization job.

## Login Theme

The `banking` theme customizes the Keycloak login page while keeping Keycloak responsible for the authentication flow.

Current implemented scope:

```txt
login/template.ftl
login/login.ftl
login/error.ftl
login/info.ftl
login/login-page-expired.ftl
login/logout-confirm.ftl
login/theme.properties
login/messages
login/resources/css
login/resources/img
login/resources/js
```

Current browser-facing theme coverage:

```txt
Login
Invalid credentials
Generic error
Information/result page
Expired login page
Logout confirmation
```

Public self-registration, public password recovery, and Keycloak remember-me are disabled in the imported realm. In a banking product, those flows should be modeled as controlled customer operations before exposing them from the login screen.

The browser-facing login should be tested through the BFF:

```txt
http://localhost:8085/web/me
```

If the realm already exists in the Docker volume, select the theme manually in Keycloak or recreate the local volume.
