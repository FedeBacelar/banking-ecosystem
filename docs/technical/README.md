# Technical Documentation

This section explains engineering decisions and conventions for the current banking ecosystem.

## Architecture

```txt
architecture/hexagonal-architecture.md
architecture/service-boundaries.md
architecture/database-per-service.md
architecture/web-access-model.md
architecture/error-handling.md
architecture/validation.md
architecture/testing-strategy.md
```

## Security

```txt
security/identity-linking.md
security/home-banking-bff-session.md
security/swagger-oauth2.md
```

## Infrastructure

```txt
infrastructure/local-mysql.md
infrastructure/config-server.md
infrastructure/eureka-server.md
infrastructure/api-gateway.md
infrastructure/keycloak.md
infrastructure/minio.md
```

The goal is to keep services consistent while still allowing each service to evolve according to its business responsibility.
