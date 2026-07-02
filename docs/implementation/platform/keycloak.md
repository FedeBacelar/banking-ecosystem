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
- Provides initial API roles for customer and account access.
- Acts as the JWT issuer used by `api-gateway`.

## Local Runtime

Default HTTP port:

```txt
8090
```

Admin console:

```txt
http://localhost:8090
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

Current client:

```txt
banking-api
```

Current roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
```

Current local test users:

```txt
api-tester
customer-reader
customer-writer
account-reader
```

## Gateway Integration

Issuer URI:

```txt
http://localhost:8090/realms/banking-ecosystem
```

`api-gateway` validates tokens from this issuer and reads API roles from `realm_access.roles`.

## Current Limitations

Business services are not protected directly yet.

Current protection is enforced at the gateway boundary.
