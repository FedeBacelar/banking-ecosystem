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
```

## Service Documentation

- [customer-service](services/customer-service/README.md)
- [account-service](services/account-service/README.md)
- [identity-service](services/identity-service/README.md)

## Platform Documentation

- [config-server](platform/config-server.md)
- [eureka-server](platform/eureka-server.md)
- [api-gateway](platform/api-gateway.md)
- [keycloak](platform/keycloak.md)

## Documentation Rule

Each service folder only documents that service.

Cross-service decisions and platform components are documented in ecosystem-level folders.
