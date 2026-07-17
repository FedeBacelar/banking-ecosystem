# Implementation Documentation

This section documents what exists in the repository today.

It is intentionally different from the business and technical documentation:

- Business docs explain what the banking concepts mean.
- Technical docs explain the architecture and engineering rules.
- Implementation docs explain the current code, endpoints, tables, tests, and integrations.

## Current Services

```txt
customer-service
account-service
identity-service
notification-service
document-service
onboarding-service
home-banking-bff
```

## Current Frontends

```txt
banking-web
```

## Service Documentation

- [customer-service](services/customer-service/README.md)
- [account-service](services/account-service/README.md)
- [identity-service](services/identity-service/README.md)
- [notification-service](services/notification-service/README.md)
- [document-service](services/document-service/README.md)
- [onboarding-service](services/onboarding-service/README.md)
- [home-banking-bff](services/home-banking-bff/README.md)

## Frontend Documentation

- [banking-web](frontends/banking-web.md)

## Platform Documentation

- [config-server](platform/config-server.md)
- [eureka-server](platform/eureka-server.md)
- [api-gateway](platform/api-gateway.md)
- [keycloak](platform/keycloak.md)

## Local Runtime Infrastructure

- [MySQL](../technical/infrastructure/local-mysql.md)
- [MinIO](../technical/infrastructure/minio.md)
- [Mailpit](../technical/infrastructure/mailpit.md)
- [Observability](../technical/infrastructure/observability.md)

## Documentation Rule

Each service folder only documents that service.

Cross-service decisions and platform components are documented in ecosystem-level folders.
