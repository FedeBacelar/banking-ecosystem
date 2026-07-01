# Local Keycloak

Local Keycloak infrastructure for the banking ecosystem.

Keycloak is the Identity Provider that will be used by future security features. It is available as local infrastructure and imports a development realm for API security work. It is not yet integrated with `api-gateway`.

The included credentials are development defaults. To override them, create a local `.env` file from `.env.example`. Local `.env` files must not be committed.

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

Client:

```txt
banking-api
```

Realm roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
```

Local test user:

```txt
Username: api-tester
Password: api-tester-password
```

The test user exists only to request local tokens while developing the gateway security integration.

## Token Request

After Keycloak starts, a local access token can be requested with:

```powershell
$response = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "banking-api"
    username = "api-tester"
    password = "api-tester-password"
  }

$response.access_token
```

This password grant is enabled only for local API testing. Production user-facing applications should use Authorization Code Flow with PKCE.

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
