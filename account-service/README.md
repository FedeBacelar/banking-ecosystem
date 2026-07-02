# account-service

Account API for the banking ecosystem.

This service owns bank accounts, account identifiers, account lifecycle, and operational balances. It validates customers through `customer-service` before opening an account.

## Responsibility

- Open accounts for eligible customers.
- Generate account numbers.
- Generate CBU values.
- Store and update account aliases.
- Store account type, currency, status, and lifecycle history.
- Initialize account balances.
- Expose account, balance, and status history queries.
- Manage account lifecycle.
- Consume `customer-service` through Feign using Eureka service discovery.
- Forward the incoming `Authorization` header to internal Feign calls.
- Persist its own data in MySQL through Flyway migrations.
- Read operational configuration from Config Server.
- Validate JWT access tokens issued by Keycloak.

This service does not own customer personal data, KYC, transactions, cards, loans, or exchange rates.

## Requirements

- Java 21
- Docker Desktop
- Maven Wrapper included in the service
- Local MySQL infrastructure
- Config Server for centralized configuration
- Eureka Server for normal local execution
- `customer-service` running for account opening flows

## Local Database

From the ecosystem root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d
```

Local defaults are served through `config-server` from `config-repository/account-service.yaml`.

Environment overrides:

```txt
ACCOUNT_DB_URL
ACCOUNT_DB_USERNAME
ACCOUNT_DB_PASSWORD
```

## Run

Start Config Server first:

```powershell
cd ..\config-server
.\mvnw.cmd spring-boot:run
```

Start Eureka:

```powershell
cd ..\eureka-server
.\mvnw.cmd spring-boot:run
```

Start `customer-service`:

```powershell
cd ..\customer-service
.\mvnw.cmd spring-boot:run
```

Then start this service:

```powershell
cd ..\account-service
.\mvnw.cmd spring-boot:run
```

Local endpoints:

```txt
API:          http://localhost:8081
Swagger:      http://localhost:8081/swagger-ui.html
OpenAPI JSON: http://localhost:8081/v3/api-docs
Health:       http://localhost:8081/actuator/health
Info:         http://localhost:8081/actuator/info
```

Protected API permissions:

```txt
GET        /accounts/** -> ACCOUNT_READ
POST/PATCH /accounts/** -> ACCOUNT_WRITE
```

`/actuator/health` and `/actuator/info` are public for local operational checks.

Swagger and OpenAPI docs are public only when:

```txt
banking.security.public-docs-enabled=true
```

The local config repository enables this through `PUBLIC_DOCS_ENABLED`, defaulting to `true`.

Swagger UI supports OAuth2 login with Keycloak through the `banking-swagger` client and PKCE. Use `Authorize` in Swagger and log in with a user that has the required account roles.

## API

```txt
POST  /accounts
GET   /accounts/{accountId}
GET   /accounts/by-number/{accountNumber}
GET   /accounts/by-alias/{alias}
GET   /accounts/customer/{customerId}
GET   /accounts/{accountId}/balance
GET   /accounts/{accountId}/status-history
PATCH /accounts/{accountId}/alias
PATCH /accounts/{accountId}/activate
PATCH /accounts/{accountId}/freeze
PATCH /accounts/{accountId}/unfreeze
PATCH /accounts/{accountId}/close
```

## Tests

```powershell
.\mvnw.cmd test
```

The test suite covers domain rules, account opening use cases, web adapter behavior, service-level security, and persistence with MySQL Testcontainers.

## Documentation

Central documentation:

```txt
../docs/implementation/services/account-service/README.md
../docs/business/services/account-service/README.md
```
