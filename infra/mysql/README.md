# Local MySQL

Local MySQL infrastructure for the banking services.

The included credentials are development defaults. To override them, create a local `.env` file from `.env.example`. Local `.env` files must not be committed.

## Start

From the repository root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d
```

With custom local variables:

```powershell
docker compose --env-file infra/mysql/.env -f infra/mysql/docker-compose.yml up -d
```

## Stop

```powershell
docker compose -f infra/mysql/docker-compose.yml down
```

## Connection Data

Customer database:

```txt
Host: localhost
Port: 3307
Database: customer_db
Username: customer_user
Password: customer_password
JDBC URL: jdbc:mysql://localhost:3307/customer_db
```

Account database:

```txt
Host: localhost
Port: 3308
Database: account_db
Username: account_user
Password: account_password
JDBC URL: jdbc:mysql://localhost:3308/account_db
```

Root user:

```txt
Username: root
Password: root_password
```

The containers use MySQL `8.4`.

Each container exposes internal MySQL port `3306`. The published host ports are `3307` for `customer-service` and `3308` for `account-service`.
