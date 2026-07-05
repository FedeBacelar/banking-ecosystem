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
home-banking-user -> CUSTOMER_READ, ACCOUNT_READ, IDENTITY_READ, used as the browser customer user
```

`banking-admin` is not a home banking customer. It can test internal APIs, but it should not be used as the browser user for `/web/me`.

`home-banking-user` represents the typical browser user. It must access banking data through `home-banking-bff`, which resolves the linked `customerId` from the authenticated Keycloak subject.

## Local Token Request

After Keycloak starts, a local access token can be requested for manual API checks:

```powershell
function Get-BankingAccessToken {
  param(
    [string] $Username,
    [string] $Password
  )

  $response = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
      grant_type = "password"
      client_id = "banking-api"
      username = $Username
      password = $Password
    }

  $response.access_token
}

$adminToken = Get-BankingAccessToken "banking-admin" "banking-admin-password"
$homeBankingToken = Get-BankingAccessToken "home-banking-user" "home-banking-user-password"
```

This password grant is enabled only for local API testing. Production user-facing applications should use Authorization Code Flow with PKCE.

Do not use password grant to simulate home banking browser login. Use `http://localhost:8085/web/me` and login with `home-banking-user`.

## BFF Login Client

`home-banking-bff` is the local confidential client used by the browser-facing backend.

Local redirect URLs:

```txt
http://localhost:8085/web/login/oauth2/code/keycloak
http://localhost:8086/web/login/oauth2/code/keycloak
```

Local post logout redirect URLs:

```txt
http://localhost:8085/web/session
http://localhost:8086/web/session
```

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

## Gateway Authorization Checks

After the full local ecosystem is running, use the generated tokens to verify gateway authorization:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8085/api/customers/{customerId}" `
  -Headers @{ Authorization = "Bearer $homeBankingToken" }
```

Expected: `200 OK` when the customer exists.

For negative authorization checks, create temporary local users manually with narrower roles instead of keeping extra users in the imported realm.

## Realm Import Behavior

The realm import file is:

```txt
infra/keycloak/realms/banking-ecosystem-realm.json
```

Keycloak imports realm files only when the realm does not already exist in its data volume.

To recreate the local realm from the JSON file, remove the volume:

```powershell
docker compose -f infra/keycloak/docker-compose.yml down -v
docker compose -f infra/keycloak/docker-compose.yml up -d
```
