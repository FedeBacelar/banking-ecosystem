# Local Keycloak

Local Keycloak infrastructure for the banking ecosystem.

Keycloak is the Identity Provider that will be used by future security features. In this first step it is only available as local infrastructure; it is not yet integrated with `api-gateway`.

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

## Current Scope

This infrastructure step does not create realms, clients, users, or roles yet.

Those items will be introduced in a later feature when the gateway is configured as an OAuth2 Resource Server.
