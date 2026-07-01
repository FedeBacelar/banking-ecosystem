# api-gateway

API Gateway for the banking ecosystem.

This is a platform service. It does not own banking data and does not implement customer or account business rules.

## Responsibility

`api-gateway` is the HTTP entry point for external clients.

Current responsibilities:

- Route external HTTP requests to internal services.
- Keep public API paths centralized.
- Resolve service destinations through Eureka using logical service names.
- Prepare the ecosystem for future cross-cutting concerns such as authentication, authorization, rate limiting, and request tracing.

## Local Runtime

Default port:

```txt
8085
```

Base URL:

```txt
http://localhost:8085
```

## Current Routes

```txt
/customers/** -> lb://customer-service
/accounts/**  -> lb://account-service
```

The `lb://` prefix means the gateway resolves the target service through Eureka instead of using a fixed host and port.

## Configuration

The service reads operational configuration from Config Server.

Config source:

```txt
../config-repository/api-gateway.yaml
```

Local bootstrap config remains in:

```txt
src/main/resources/application.yaml
```

## Run

Recommended local startup order:

```txt
1. MySQL containers
2. config-server
3. eureka-server
4. customer-service
5. account-service
6. api-gateway
```

From this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd test
```
