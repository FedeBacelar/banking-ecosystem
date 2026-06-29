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

This service does not own authentication, accounts, balances, transactions, cards, loans, exchange rates, or external integrations.

## Requirements

- Java 21
- Docker Desktop
- Maven Wrapper included in the service
- Local MySQL infrastructure
- Eureka Server for normal local execution

## Local Database

From the ecosystem root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d
```

Local defaults:

```txt
URL: jdbc:mysql://localhost:3307/customer_db
User: customer_user
Password: customer_password
```

Environment overrides:

```txt
CUSTOMER_DB_URL
CUSTOMER_DB_USERNAME
CUSTOMER_DB_PASSWORD
```

## Run

Start Eureka first:

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

The test suite covers domain rules, use cases, web adapter behavior, persistence with MySQL Testcontainers, and an HTTP end-to-end flow.

## Documentation

Central documentation:

```txt
../docs/implementation/services/customer-service/README.md
../docs/business/services/customer-service/README.md
```
