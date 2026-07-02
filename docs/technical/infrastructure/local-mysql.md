# Local MySQL Infrastructure

Local MySQL is managed in:

```txt
infra/mysql/docker-compose.yml
```

## Current Containers

```txt
customer-mysql
account-mysql
identity-mysql
```

Each container runs MySQL internally on port `3306`.

The host ports are different:

```txt
localhost:3307 -> customer-mysql:3306
localhost:3308 -> account-mysql:3306
localhost:3309 -> identity-mysql:3306
```

## Databases

```txt
customer_db
account_db
identity_db
```

## Why Separate Containers?

Using separate containers makes the database-per-service principle visible in local development.

It also reduces the chance of accidentally coupling services through one shared schema.

## Start

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d
```

## Stop

```powershell
docker compose -f infra/mysql/docker-compose.yml down
```

## Local Customization

Ports and credentials can be overridden with environment variables or a local `.env` file.

`.env` files should not be committed.
