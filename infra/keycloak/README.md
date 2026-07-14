# Local Keycloak

Local Keycloak infrastructure for the banking ecosystem.

Keycloak is the local Identity Provider for API security work. It imports a development realm used by `api-gateway` and protected backend services.

The included credentials are development defaults. To override machine-client
values locally, create a `.env` file from `.env.example`. Local `.env` files
must not be committed. The normal local SMTP flow remains credential-free and
uses Mailpit; do not store a real SMTP account in this workspace file.

The local realm uses the `banking` login and email themes located in:

```txt
infra/keycloak/themes/banking/login
infra/keycloak/themes/banking/email
```

## Start

From the repository root:

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
docker compose -f infra/keycloak/docker-compose.yml up -d
```

With custom local variables:

```powershell
docker compose --env-file infra/keycloak/.env -f infra/keycloak/docker-compose.yml up -d
```

The default Keycloak SMTP target is Mailpit. An ignored `.env` copied from the
example may override local machine-client secrets and other development
settings, but its SMTP values should stay on Mailpit with empty credentials.
Production SMTP credentials are injected by the deployment runtime or a secrets
manager, never copied into the repository workspace.

The development machine-client defaults match the service configuration; any
override must also be supplied to the corresponding Java process. Realm JSON
keeps secret placeholders only, and the one-shot initializer reconciles client
secrets on every infrastructure start so existing volumes can receive rotations
safely.

The one-shot `banking-keycloak-realm-init` container applies the restricted banking User Profile after the realm becomes healthy. Its expected steady state is `Exited (0)`.

Keycloak's local public identity URL is fixed to the complete
`http://localhost:8090` origin through `KEYCLOAK_PUBLIC_URL`. It uses that
canonical URL when it creates signed action links instead of deriving customer
email links from an incoming request host. A deployed environment must set its
own complete HTTPS `KEYCLOAK_PUBLIC_URL`.

## Stop

```powershell
docker compose -f infra/keycloak/docker-compose.yml down
```

## Admin Console

```txt
URL: http://localhost:8090
Username: admin
Password: admin
```

## Imported Realm

Realm:

```txt
banking-ecosystem
```

Login theme:

```txt
banking
```

Email theme:

```txt
banking
```

Clients:

```txt
banking-api
banking-swagger
home-banking-bff
onboarding-bff-service
home-banking-bff-service
onboarding-orchestrator
account-service
```

Realm roles:

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

Local test users:

```txt
banking-admin / banking-admin-password
home-banking-user / home-banking-user-password
```

These users have different purposes. They are not interchangeable.

Role summary:

```txt
banking-admin     -> all current API capability roles, used for local operational/API testing
home-banking-user -> HOME_BANKING_USER, used only through the browser/BFF login flow
```

The interactive `home-banking-bff` client has no service account. Backend calls use separate confidential clients:

```txt
onboarding-bff-service   -> ONBOARDING_READ, ONBOARDING_WRITE
home-banking-bff-service -> CUSTOMER_READ, ACCOUNT_READ, IDENTITY_READ
account-service          -> CUSTOMER_READ
onboarding-orchestrator  -> read + purpose-specific provisioning roles, document and notification access
```

These service accounts are not human login users and are not shared between purposes.

The dedicated `onboarding-orchestrator` confidential client is used by `onboarding-service` for durable provisioning. It receives `CUSTOMER_PROVISION`, `ACCOUNT_PROVISION` and `IDENTITY_PROVISION` instead of the broader write roles. It does not reuse the browser token or a human test user.

`ONBOARDING_OPERATE` is reserved for explicit operational actions. The browser-facing onboarding service account never receives it; the local `banking-admin` user does.

The realm initializer creates missing provisioning roles and the `account-service` client, rotates local machine secrets, and reconciles the exact application-role set for every managed service account on existing volumes.

`banking-admin` is not a home banking customer. It can test internal APIs, but it should not be used as the browser user for `/web/me`.

`home-banking-user` represents the typical browser user. It must access banking data through `home-banking-bff`, which resolves the linked `customerId` from the authenticated Keycloak subject.

Direct Access Grants are disabled. Browser login uses Authorization Code Flow through `http://localhost:8085/web/oauth2/authorization/keycloak`; machine access uses client credentials and injected secrets.

## BFF Login Client

`home-banking-bff` is the local confidential client used by the browser-facing backend.

Local redirect URLs:

```txt
http://localhost:8085/web/login/oauth2/code/keycloak
```

Local post logout redirect URLs:

```txt
http://localhost:4200/*
```

Use the gateway URL on port `8085` for browser login. Port `8086` is the BFF service port and is reserved for diagnostics, not the normal browser callback.

The development client secret is intentionally local-only. Real environments must inject the secret from outside the repository.

## Banking Login Theme

The banking theme customizes the Keycloak browser authentication experience.

Theme path:

```txt
infra/keycloak/themes/banking/login
```

Current scope:

```txt
template.ftl
login.ftl
error.ftl
info.ftl
login-page-expired.ftl
logout-confirm.ftl
theme.properties
messages/messages_es.properties
resources/css/nerva-tokens.css
resources/css/banking-login.css
resources/img/banking-logo.svg
resources/fonts/geist-latin-wght-normal.woff2
resources/js/banking-login.js
```

The credential required actions add dedicated `login-update-profile.ftl` and
`login-update-password.ftl` screens. They preserve Keycloak's native field and
form contracts while presenting only the username and password choices needed
by onboarding. The realm explicitly orders `UPDATE_PROFILE` before
`UPDATE_PASSWORD`, so customers choose their username before creating a
password on both fresh imports and existing local volumes.

The customer-facing realm and theme support Spanish only. The theme uses the canonical Nerva tokens, logos, and self-hosted Geist font generated from `design-system`; run `node design-system/scripts/generate.mjs --check` from the repository root to verify the copies.

The login form uses direct access copy and always shows the academic disclaimer. It does not advertise unimplemented product features or use security/implementation language as marketing content.

The theme keeps Keycloak in control of authentication behavior: form submission, validation errors, redirects, password visibility, social providers, and session handling. JavaScript adds accessible loading feedback and password visibility as progressive enhancements; native form submission remains available without JavaScript.

Public self-registration, public password recovery, and Keycloak remember-me are disabled in the imported realm. Those flows should be implemented only after defining the corresponding banking business process.

The Docker Compose file mounts themes into the container:

```txt
./themes:/opt/keycloak/themes:ro
```

For local theme development, Docker Compose disables Keycloak theme/template/static caching. Restart the Keycloak container after changing mounted files if the browser still shows an older version.

`BANKING_FRONTEND_URL` controls the theme's `Volver al inicio` link and the interactive client's browser URLs. Use an origin without a trailing slash; the local default is `http://localhost:4200`.

The frontend and identity origins used by notification action-link validation
are independent exact origins. They contain scheme, host, and effective port;
wildcard hosts and suffix matching are not supported. The identity origin must
match `KEYCLOAK_PUBLIC_URL`.

The one-shot `keycloak-realm-init` container reconciles the login and email themes, Spanish locale, credential-action order, password policy, browser URLs, SMTP sender, and client secrets on every infrastructure start. This also updates existing local volumes.

## Credential password policy

The customer realm accepts passwords from 15 to 64 characters, rejects the
username and email as the complete password, and checks the repository-owned
local blocklist. It does not require arbitrary character classes or periodic
password changes. Spaces and passphrases remain supported.

```txt
length(15) and maxLength(64) and notUsername and notEmail and passwordBlacklist(nerva-passwords.txt)
```

The local blocklist is mounted read-only from
`infra/keycloak/password-blacklists`. It makes development and validation
deterministic; it is not a production compromised-password corpus.

## Credential email theme

The `banking/email` theme owns the credential invitation subject, responsive
HTML and plain-text alternative. Copy describes the two customer actions
without exposing Keycloak action identifiers or administrative language, and
the academic disclaimer remains visible even when images are unavailable.

Keycloak owns the complete credential action URL. `execute-actions-email`
creates and signs the link from `KEYCLOAK_PUBLIC_URL`, while the
`home-banking-bff` client constrains the post-action redirect to
`http://localhost:8085/web/auth/login/onboarding-completion`. The theme only
presents the generated URL; `notification-service` does not reconstruct it.

Locally, Keycloak sends this message to Mailpit without SMTP authentication,
STARTTLS, or SSL. Real SMTP settings are deployment secrets and do not belong in
the ignored local `.env` used by the development journey.

Verify the static theme contract, or include the rendered local Keycloak page when the container is running:

```powershell
node infra/keycloak/scripts/verify-theme.mjs
node infra/keycloak/scripts/verify-theme.mjs --live
node infra/keycloak/scripts/verify-authorization.mjs
node infra/keycloak/scripts/verify-authorization.mjs --live
node infra/keycloak/scripts/verify-credential-flow.mjs
```

The credential-flow verifier creates and removes a disposable Keycloak user,
sends one message through the configured local SMTP server, checks both email
representations, exercises the username and password required actions, and
stops at the BFF completion redirect. It requires Keycloak and Mailpit to be
running.

Manual verification path:

```txt
Realm settings -> Themes -> Login theme -> banking
Realm settings -> Themes -> Email theme -> banking
```

## Public Boundary Check

The gateway exposes only `/web/**`. A direct request such as `/api/customers/{id}` must be denied; authenticated customer data is composed by `home-banking-bff` after resolving the Keycloak subject.

Internal authorization is verified at each service with the dedicated machine clients imported in this realm. Do not create one shared human-style service user.

## Realm Import Behavior

The realm import file is:

```txt
infra/keycloak/realms/banking-ecosystem-realm.json
```

Keycloak imports realm files only when the realm does not already exist in its data volume.

Only `banking-ecosystem-realm.json` is mounted in Keycloak's import directory. `banking-user-profile.json` is intentionally applied afterward through the Admin API and must not be treated as a realm import file.

To recreate the local realm from the JSON file, remove the volume:

```powershell
docker compose -f infra/keycloak/docker-compose.yml down -v
docker compose -f infra/keycloak/docker-compose.yml up -d
```
