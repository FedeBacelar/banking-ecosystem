# customer-service Implementation

`customer-service` is the current customer bounded context.

It owns customer identity and the formal customer relationship with the bank. Today it supports natural person registration, customer queries, KYC status changes, and customer lifecycle changes.

## Current Status

Functional current implementation.

Implemented:

- Register natural person customers.
- Generate customer numbers.
- Store party, natural person, document, contact, address, KYC, and customer status data.
- Query customers by id, document, and customer number.
- Query customer status history.
- Approve or reject KYC.
- Suspend, reactivate, and close customers.
- Validate request sizes against database limits.
- Validate JWT access tokens issued by Keycloak.
- Return consistent `ProblemDetail` errors.
- Run unit, web adapter, persistence, and E2E tests.

## Local Runtime

Operational configuration is served by config-server from config-repository/customer-service.yaml.


Default HTTP port:

```txt
8080
```

Swagger:

```txt
http://localhost:8080/swagger-ui.html
```

Required API roles:

```txt
GET   /customers/**                   -> CUSTOMER_READ
POST  /customers/natural-persons      -> CUSTOMER_PROVISION
PATCH /customers/{customerId}/kyc/approve -> CUSTOMER_PROVISION
other POST/PATCH /customers/**         -> CUSTOMER_WRITE
```

The onboarding orchestrator receives `CUSTOMER_PROVISION`, not the broader `CUSTOMER_WRITE` capability.

Local documentation endpoints are public when `banking.security.public-docs-enabled=true`.

Swagger UI can authenticate against Keycloak using the `banking-swagger` client with Authorization Code and PKCE.

Database:

```txt
customer_db on localhost:3307
```

## Related Docs

- [API](api.md)
- [Database](database.md)
- [Tests](tests.md)
- Business docs: [customer-service](../../../business/services/customer-service/README.md)

