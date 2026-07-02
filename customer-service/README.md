# customer-service

Customer API for the banking ecosystem.

This service owns the customer's formal relationship with the bank, natural person onboarding, customer lifecycle, and initial KYC state.

## Responsibility

- Register natural person customers.
- Store customer identity, primary document, contact data, address data, and KYC profile.
- Expose customer queries by id, document, and customer number.
- Manage KYC approval/rejection.
- Manage customer lifecycle.
- Persist its own data in MySQL through Flyway migrations.
- Register itself in Eureka for service discovery.
- Read operational configuration from Config Server.
- Validate JWT access tokens issued by Keycloak.

This service does not own authentication, accounts, balances, transactions, cards, loans, exchange rates, or external integrations.

## Requirements

- Java 21
- Docker Desktop
- Maven Wrapper included in the service
- Local MySQL infrastructure
- Config Server for centralized configuration
- Eureka Server for normal local execution

## Local Database

From the ecosystem root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d
```

Local defaults are served through `config-server` from `config-repository/customer-service.yaml`.

Environment overrides:

```txt
CUSTOMER_DB_URL
CUSTOMER_DB_USERNAME
CUSTOMER_DB_PASSWORD
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

Then start this service:

```powershell
cd ..\customer-service
.\mvnw.cmd spring-boot:run
```

Local endpoints:

```txt
API:          http://localhost:8080
Swagger:      http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
Health:       http://localhost:8080/actuator/health
Info:         http://localhost:8080/actuator/info
```

Protected API permissions:

```txt
GET        /customers/** -> CUSTOMER_READ
POST/PATCH /customers/** -> CUSTOMER_WRITE
```

`/actuator/health` and `/actuator/info` are public for local operational checks.

Swagger and OpenAPI docs are public only when:

```txt
banking.security.public-docs-enabled=true
```

The local config repository enables this through `PUBLIC_DOCS_ENABLED`, defaulting to `true`.

Swagger UI supports OAuth2 login with Keycloak through the `banking-swagger` client and PKCE. Use `Authorize` in Swagger and log in with a user that has the required customer roles.

## API

```txt
POST  /customers/natural-persons
GET   /customers/{customerId}
GET   /customers/by-document?type=DNI&number=...&country=AR
GET   /customers/by-number/{customerNumber}
GET   /customers/{customerId}/status-history
PATCH /customers/{customerId}/kyc/approve
PATCH /customers/{customerId}/kyc/reject
PATCH /customers/{customerId}/suspend
PATCH /customers/{customerId}/reactivate
PATCH /customers/{customerId}/close
```

Customer registration creates a customer in `PENDING_KYC` with KYC status `PENDING_REVIEW`.

Invalid lifecycle transitions return `409 Conflict` with `ProblemDetail`.

## Tests

```powershell
.\mvnw.cmd test
```

The test suite covers domain rules, use cases, web adapter behavior, service-level security, persistence with MySQL Testcontainers, and an HTTP end-to-end flow.

## Documentation

Central documentation:

```txt
../docs/implementation/services/customer-service/README.md
../docs/business/services/customer-service/README.md
```
