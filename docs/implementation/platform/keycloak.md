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
- Provides initial API roles for customer, account, and identity access.
- Issues JWT access tokens validated by `api-gateway`, `customer-service`, `account-service`, and `identity-service`.

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

Current clients:

```txt
banking-api
banking-swagger
```

Current roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
IDENTITY_READ
IDENTITY_WRITE
```

Current local test users:

```txt
api-tester
customer-reader
customer-writer
account-reader
identity-admin
```

## Service Integration

Current protected components:

```txt
api-gateway
customer-service
account-service
identity-service
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
```

## Swagger Client

`banking-swagger` is used by service Swagger UIs.

It is a public client configured for Authorization Code with PKCE.

Local redirect URLs:

```txt
http://localhost:8080/swagger-ui/oauth2-redirect.html
http://localhost:8081/swagger-ui/oauth2-redirect.html
http://localhost:8082/swagger-ui/oauth2-redirect.html
```

If a local Keycloak volume already existed before this client was added, add the client manually or recreate the local Keycloak volume.
