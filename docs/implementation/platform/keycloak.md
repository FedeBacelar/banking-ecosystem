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
- Provides separate BFF, onboarding-orchestrator, and account-service machine clients.
- Provides read, general write, provisioning, and onboarding-operation capabilities without sharing browser tokens.
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

Deployment boundary model:

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
onboarding-bff-service
home-banking-bff-service
onboarding-orchestrator
account-service
```

Current roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
CUSTOMER_PROVISION
ACCOUNT_READ
ACCOUNT_WRITE
ACCOUNT_PROVISION
IDENTITY_READ
IDENTITY_WRITE
IDENTITY_PROVISION
NOTIFICATION_WRITE
DOCUMENT_READ
DOCUMENT_WRITE
ONBOARDING_READ
ONBOARDING_WRITE
ONBOARDING_OPERATE
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
CUSTOMER_PROVISION -> create the onboarding customer and approve initial KYC
ACCOUNT_READ   -> read account API
ACCOUNT_WRITE  -> write account API
ACCOUNT_PROVISION -> create and activate the onboarding account
IDENTITY_READ  -> read/resolve identity links
IDENTITY_WRITE -> activate/disable existing identity links
IDENTITY_PROVISION -> create the initial onboarding identity link
NOTIFICATION_WRITE -> send internal notifications
DOCUMENT_READ -> read document metadata
DOCUMENT_WRITE -> upload documents
ONBOARDING_READ -> read onboarding application metadata and validate continuations
ONBOARDING_WRITE -> start onboarding applications and consume magic links
ONBOARDING_OPERATE -> retry failed review or provisioning work
```

The role model separates applicant operations, general maintenance, initial provisioning, and operational retry. The BFF still enforces the customer-facing "own data" flow by deriving the `customerId` from the authenticated identity link and not accepting a customer id from the browser. Business services authorize API capabilities by role; they do not independently derive customer ownership from a human subject.

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

The realm import creates `banking-swagger` for a fresh realm. The one-shot initializer reconciles the authorization additions on existing volumes, but does not recreate this interactive client if it is manually deleted.

## BFF Client

`home-banking-bff` is used by the browser-facing backend.

It is a confidential client configured for Authorization Code Flow.

Local redirect URLs:

```txt
http://localhost:8085/web/login/oauth2/code/keycloak
```

Local post logout redirect URLs:

```txt
http://localhost:4200/*
```

The browser-facing local flow must use the gateway URL on port `8085`. Direct BFF URLs on port `8086` are only for diagnostics and should not be registered as the normal browser callback.

The local secret in the realm import is only a development default. Real secrets must come from environment variables or a secrets manager.

The interactive `home-banking-bff` client has service accounts disabled. Machine access is separated into confidential clients:

```txt
onboarding-bff-service   -> ONBOARDING_READ, ONBOARDING_WRITE
home-banking-bff-service -> IDENTITY_READ, CUSTOMER_READ, ACCOUNT_READ
account-service          -> CUSTOMER_READ
```

The browser never uses these clients and the BFF never forwards the user's OIDC access token into the service graph.

`account-service` replaces any inbound bearer token before its customer lookup and obtains a token for its own machine client. That nested call therefore cannot inherit the onboarding orchestrator's provisioning authority.

The onboarding facade does not call document or notification services. It sends one composite submission to `onboarding-service`, which owns that orchestration.

## Onboarding Orchestrator Client

`onboarding-orchestrator` is a dedicated client-credentials client used only by `onboarding-service`. Its current realm roles are:

```txt
CUSTOMER_READ
CUSTOMER_PROVISION
ACCOUNT_READ
ACCOUNT_PROVISION
IDENTITY_READ
IDENTITY_PROVISION
DOCUMENT_READ
DOCUMENT_WRITE
NOTIFICATION_WRITE
```

It also receives only the Keycloak realm-management roles required to query and manage onboarding users. It is not a shared human user.

`ONBOARDING_OPERATE` is not assigned to either the onboarding BFF or the orchestrator. The local `banking-admin` user owns that explicit capability for protected review and provisioning retries.

## Credential Setup

Approved applicants are created with provisional username `pending-{applicationId}`, verified email, customer realm roles, and required actions `UPDATE_PROFILE` and `UPDATE_PASSWORD`.

Keycloak sends the action link itself through `execute-actions-email`; identity tokens are not transported through `notification-service`. SMTP values and the orchestrator secret are environment placeholders. Local values live in ignored `infra/keycloak/.env`; safe names live in `.env.example`.

The one-shot `keycloak-realm-init` container applies `banking-user-profile.json` after realm startup. The profile permits the applicant to choose the username while email, first name, and last name remain visible but user read-only. Unmanaged attributes remain on Keycloak's strict `DISABLED` default; the managed profile does not enable arbitrary user attributes.

The credential action lifespan defaults to 24 hours and redirects to:

```txt
http://localhost:8085/web/auth/login/onboarding-completion
```

That BFF entry point creates an authenticated session and returns to Angular only after OAuth completes. The previous `/onboarding/credentials-complete` frontend route remains temporarily as a fixed, parameter-free alias for action emails that were already issued.

When `keycloak-realm-init` exits with code `0`, that stopped container is expected; it is an initialization job.

## Login Theme

The `banking` theme customizes the Keycloak login page while keeping Keycloak responsible for the authentication flow.

The customer-facing theme is Spanish-only, uses direct sign-in copy, and shows
the academic disclaimer beside the form on every viewport. It does not present
unimplemented banking capabilities or implementation/security language as
customer benefits.

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

No custom email theme is configured. Identity emails currently use Keycloak's inherited email templates with the configured realm sender.

Public self-registration, public password recovery, and Keycloak remember-me are disabled in the imported realm. In a banking product, those flows should be modeled as controlled customer operations before exposing them from the login screen.

The browser-facing login should be tested through the BFF:

```txt
http://localhost:8085/web/auth/login/home
```

`keycloak-realm-init` reconciles the theme, Spanish locale, browser URLs, client secrets, capability roles, the `account-service` machine client, and exact application-role mappings for both new realms and existing local volumes.
