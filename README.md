# banking-ecosystem

Professional banking microservices ecosystem built with Java, Spring Boot, Spring Cloud, and MySQL.

The repository is organized as a monorepo. Each service owns its code, configuration, tests, and local README, while shared infrastructure and centralized documentation live at the repository root.

## Structure

- `customer-service`: formal customer registration, customer lifecycle, and KYC state.
- `account-service`: bank accounts, account identifiers, account lifecycle, balances, and customer validation.
- `identity-service`: links authenticated external identities to internal banking customers.
- `notification-service`: notification requests, templates, delivery attempts, and email delivery state.
- `document-service`: document metadata and object storage integration for banking evidence.
- `onboarding-service`: digital onboarding applications, email magic links, and onboarding state.
- `home-banking-bff`: browser-facing session boundary, onboarding facade, and customer data composition.
- `banking-web`: customer-facing Angular SPA for public journeys and authenticated home banking.
- `config-server`: centralized configuration server for the local ecosystem.
- `eureka-server`: service discovery server for the local microservices ecosystem.
- `api-gateway`: external HTTP entry point and route layer for the local ecosystem.
- `config-repository`: local configuration source served by `config-server`.
- `infra/mysql`: local MySQL containers, one database per business service.
- `infra/keycloak`: local OAuth2/OpenID Connect identity provider, service clients, and banking login theme.
- `infra/minio`: local S3-compatible object storage for banking documents.
- `infra/mailpit`: local SMTP capture server and email inspection UI.
- `infra/observability`: local metrics, logs, traces, and Grafana dashboards for the onboarding journey.
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
- Onboarding DB port: `3312`
- Onboarding DB: `onboarding_db`

To override local values, create `infra/mysql/.env` from `infra/mysql/.env.example` and run:

```powershell
docker compose --env-file infra/mysql/.env -f infra/mysql/docker-compose.yml up -d
```

Local `.env` files must not be committed.

Start Mailpit before the components that send customer email:

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
```

Mailpit captures messages instead of sending them to Internet recipients:

```txt
SMTP: localhost:1025
Inbox: http://localhost:8025
```

It is the local SMTP default for both `notification-service` and Keycloak.

Start Keycloak local infrastructure:

```powershell
docker compose -f infra/keycloak/docker-compose.yml up -d
```

Keycloak credential email works with Mailpit without real SMTP credentials. An
ignored `infra/keycloak/.env` may hold local-only overrides copied from
`.env.example`, but the normal local journey must keep the Mailpit host and
empty SMTP credentials. A deployed environment injects provider credentials
from its runtime or secrets manager instead of storing them in a repository
`.env` file.

The `keycloak-realm-init` container applies the restricted banking user profile
and is expected to stop with exit code `0` after startup.

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

Start the optional local observability stack:

```powershell
docker compose -f infra/observability/docker-compose.yml up -d
```

Grafana is available at `http://localhost:3000`. Start API Gateway, Home Banking
BFF, Onboarding, and Notification with the `observability` Spring profile to
populate the provisioned `Nerva · Onboarding` dashboard. The applications keep
OpenTelemetry disabled by default, and remain functional if the local stack is
not running. See `infra/observability/README.md` for credentials, endpoints,
retention, and verification commands.

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
http://localhost:8888/notification-service/default
http://localhost:8888/document-service/default
http://localhost:8888/onboarding-service/default
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

The local defaults send email to Mailpit without credentials or TLS. If an
ignored `notification-service/.env` is present, it should be copied from the
safe example and remain configured for Mailpit during the normal local journey.
Operating-system environment variables still take precedence. A deployed
environment must inject its provider host, port, credentials, authentication,
STARTTLS, and sender externally.

Authenticated SMTP additionally requires mandatory STARTTLS or implicit SSL,
and all SMTP operations use finite timeouts. Invalid combinations fail at
startup without exposing configuration values.

Customer email links are accepted only for configured exact origins. Local
development uses the Angular origin for onboarding links and the Keycloak
identity origin for identity links; wildcard hosts and suffix matches are not
part of the contract. Keycloak constructs and signs its own credential action
link using the fixed public URL configured by `KEYCLOAK_PUBLIC_URL`.

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

Start `onboarding-service`:

```powershell
cd ..\onboarding-service
.\mvnw.cmd spring-boot:run
```

Onboarding Swagger:

```txt
http://localhost:8087/swagger-ui.html
```

Start `home-banking-bff`:

```powershell
cd ..\home-banking-bff
.\mvnw.cmd spring-boot:run
```

BFF local path through the gateway:

```txt
http://localhost:8085/web/me
```

Public onboarding contracts, including CSRF-protected submit, status, and credential invitation resend, are available only through `http://localhost:8085/web/onboarding/**`.

Start `api-gateway`:

```powershell
cd ..\api-gateway
.\mvnw.cmd spring-boot:run
```

Gateway entry point:

```txt
http://localhost:8085
```

The gateway locally limits anonymous application starts to three requests per
minute and ten per hour for each client. A rejected request returns `429` with
`Retry-After`, which Angular presents as a concrete wait. Forwarded client
addresses are ignored unless their immediate proxy CIDR is explicitly trusted.
A process-wide ceiling also bounds address rotation. The local in-memory
limiter is for the current single-instance stage; a future multi-instance/VPS
deployment must use a shared edge limit.

Create or reconcile the local bank data linked to the Keycloak user
`home-banking-user` after Keycloak, customer, account, and identity services are
ready. The script is safe to run more than once and stops on conflicting local
data instead of replacing it:

```powershell
cd ..
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local\seed-home-banking-user.ps1
```

The local login remains:

```txt
home-banking-user / home-banking-user-password
```

Start `banking-web`:

```powershell
cd ..\banking-web
npm start
```

Angular frontend:

```txt
http://localhost:4200/
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
cd ..\onboarding-service
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

```powershell
cd ..\banking-web
npm test -- --watch=false
npm run build
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
