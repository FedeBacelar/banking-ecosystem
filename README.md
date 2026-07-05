# banking-ecosystem

Professional banking microservices ecosystem built with Java, Spring Boot, Spring Cloud, and MySQL.

The repository is organized as a monorepo. Each service owns its code, configuration, tests, and local README, while shared infrastructure and centralized documentation live at the repository root.

## Structure

- `customer-service`: customer onboarding, customer lifecycle, and initial KYC API.
- `account-service`: bank accounts, account identifiers, account lifecycle, balances, and customer validation.
- `identity-service`: links authenticated external identities to internal banking customers.
- `notification-service`: notification requests, templates, delivery attempts, and email delivery state.
- `document-service`: document metadata and object storage integration for banking evidence.
- `home-banking-bff`: browser-facing backend for future home banking sessions.
- `config-server`: centralized configuration server for the local ecosystem.
- `eureka-server`: service discovery server for the local microservices ecosystem.
- `api-gateway`: external HTTP entry point and route layer for the local ecosystem.
- `config-repository`: local configuration source served by `config-server`.
- `infra/mysql`: local MySQL containers, one database per business service.
- `infra/keycloak`: local identity provider infrastructure for future OAuth2/OpenID Connect security.
- `infra/minio`: local S3-compatible object storage for banking documents.
- `docs`: centralized business, technical, implementation, and database documentation.

## Local Infrastructure

Start MySQL containers from the repository root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d
```

Local defaults:

- Host: `localhost`
- Customer DB port: `3307`
- Customer DB: `customer_db`
- Account DB port: `3308`
- Account DB: `account_db`
- Identity DB port: `3309`
- Identity DB: `identity_db`
- Notification DB port: `3310`
- Notification DB: `notification_db`
- Document DB port: `3311`
- Document DB: `document_db`

To override local values, create `infra/mysql/.env` from `infra/mysql/.env.example` and run:

```powershell
docker compose --env-file infra/mysql/.env -f infra/mysql/docker-compose.yml up -d
```

Local `.env` files must not be committed.

Start Keycloak local infrastructure:

```powershell
docker compose -f infra/keycloak/docker-compose.yml up -d
```

Keycloak admin console:

```txt
http://localhost:8090
```

Start MinIO local object storage:

```powershell
docker compose -f infra/minio/docker-compose.yml up -d
```

MinIO console:

```txt
http://localhost:9001
```

## Run Locally

Start `config-server` first:

```powershell
cd config-server
.\mvnw.cmd spring-boot:run
```

Config Server examples:

```txt
http://localhost:8888/customer-service/default
http://localhost:8888/account-service/default
http://localhost:8888/identity-service/default
http://localhost:8888/home-banking-bff/default
http://localhost:8888/api-gateway/default
```

Start `eureka-server`:

```powershell
cd ..\eureka-server
.\mvnw.cmd spring-boot:run
```

Eureka dashboard:

```txt
http://localhost:8761
```

Start `customer-service`:

```powershell
cd ..\customer-service
.\mvnw.cmd spring-boot:run
```

Customer Swagger:

```txt
http://localhost:8080/swagger-ui.html
```

Start `account-service`:

```powershell
cd ..\account-service
.\mvnw.cmd spring-boot:run
```

Account Swagger:

```txt
http://localhost:8081/swagger-ui.html
```

Start `identity-service`:

```powershell
cd ..\identity-service
.\mvnw.cmd spring-boot:run
```

Identity Swagger:

```txt
http://localhost:8082/swagger-ui.html
```

Start `notification-service`:

```powershell
cd ..\notification-service
.\mvnw.cmd spring-boot:run
```

Notification Swagger:

```txt
http://localhost:8083/swagger-ui.html
```

Start `document-service`:

```powershell
cd ..\document-service
.\mvnw.cmd spring-boot:run
```

Document Swagger:

```txt
http://localhost:8084/swagger-ui.html
```

Start `home-banking-bff`:

```powershell
cd ..\home-banking-bff
.\mvnw.cmd spring-boot:run
```

BFF local path through the gateway:

```txt
http://localhost:8085/web/session
```

Start `api-gateway`:

```powershell
cd ..\api-gateway
.\mvnw.cmd spring-boot:run
```

Gateway entry point:

```txt
http://localhost:8085
```

## Tests

```powershell
cd config-server
.\mvnw.cmd test
```

```powershell
cd ..\eureka-server
.\mvnw.cmd test
```

```powershell
cd ..\customer-service
.\mvnw.cmd test
```

```powershell
cd ..\account-service
.\mvnw.cmd test
```

```powershell
cd ..\identity-service
.\mvnw.cmd test
```

```powershell
cd ..\notification-service
.\mvnw.cmd test
```

```powershell
cd ..\document-service
.\mvnw.cmd test
```

```powershell
cd ..\home-banking-bff
.\mvnw.cmd test
```

```powershell
cd ..\api-gateway
.\mvnw.cmd test
```

Integration tests use Testcontainers with MySQL.

## Documentation

Central documentation starts at:

```txt
docs/README.md
```

Main sections:

- `docs/business`: banking concepts, business responsibilities, states, and service boundaries.
- `docs/technical`: architecture, infrastructure, validation, error handling, and testing strategy.
- `docs/implementation`: current implementation state, endpoints, tables, tests, and integrations.
- `docs/database`: editable DBML model and visual ERD export.
