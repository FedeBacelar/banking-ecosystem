# Local Keycloak

Local Keycloak infrastructure for the banking ecosystem.

Keycloak is the local Identity Provider for API security work. It imports a development realm used by `api-gateway` and protected backend services.

The included credentials are development defaults. To override them, create a local `.env` file from `.env.example`. Local `.env` files must not be committed.

The local realm uses the `banking` login theme located in:

```txt
infra/keycloak/themes/banking/login
```

## Start

From the repository root:

```powershell
docker compose -f infra/keycloak/docker-compose.yml up -d
```

With custom local variables:

```powershell
docker compose --env-file infra/keycloak/.env -f infra/keycloak/docker-compose.yml up -d
```

Create the ignored `.env` from `.env.example` to configure Keycloak SMTP and override the local machine-client secrets. The development defaults match the service configuration; any override must also be supplied to the corresponding Java process. Realm JSON keeps placeholders only, and the one-shot initializer reconciles client secrets on every infrastructure start so existing volumes can receive rotations safely.

The one-shot `banking-keycloak-realm-init` container applies the restricted banking User Profile after the realm becomes healthy. Its expected steady state is `Exited (0)`.

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

Clients:

```txt
banking-api
banking-swagger
home-banking-bff
onboarding-orchestrator
```

Realm roles:

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
```

These service accounts are not human login users and are not shared between purposes.

The dedicated `onboarding-orchestrator` confidential client is used by `onboarding-service` for customer, account, identity, document, notification and Keycloak user-administration calls. It does not reuse the browser token or a human test user.

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
messages/messages_en.properties
resources/css/banking-login.css
resources/img/banking-logo.svg
resources/js/banking-login.js
```

The theme keeps Keycloak in control of authentication behavior: form submission, validation errors, redirects, password visibility, social providers, and session handling.

Public self-registration, public password recovery, and Keycloak remember-me are disabled in the imported realm. Those flows should be implemented only after defining the corresponding banking business process.

The Docker Compose file mounts themes into the container:

```txt
./themes:/opt/keycloak/themes:ro
```

For local theme development, Docker Compose disables Keycloak theme/template/static caching. Restart the Keycloak container after changing mounted files if the browser still shows an older version.

If the realm already exists in the Docker volume, changing `loginTheme` in the import JSON will not update the running realm automatically. Set the login theme manually in the Keycloak admin console or recreate the local volume.

Manual admin path:

```txt
Realm settings -> Themes -> Login theme -> banking
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
