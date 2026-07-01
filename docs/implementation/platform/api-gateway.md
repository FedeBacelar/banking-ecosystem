# api-gateway Implementation

`api-gateway` is the current API Gateway component.

It is a platform service. It does not own banking business data.

## Current Status

Implemented.

Current capabilities:

- Runs a Spring Cloud Gateway WebFlux server.
- Reads route configuration from Config Server.
- Registers as a Eureka client.
- Routes customer requests to `customer-service`.
- Routes account requests to `account-service`.

## Local Runtime

Default HTTP port:

```txt
8085
```

Base URL:

```txt
http://localhost:8085
```

## Configuration

Bootstrap file:

```txt
api-gateway/src/main/resources/application.yaml
```

Centralized configuration:

```txt
config-repository/api-gateway.yaml
```

Current routes:

```txt
/customers/** -> lb://customer-service
/accounts/**  -> lb://account-service
```

## Tests

Current test command:

```powershell
cd api-gateway
.\mvnw.cmd test
```

## Local Startup Order

```txt
1. infra/mysql
2. config-server
3. eureka-server
4. customer-service
5. account-service
6. api-gateway
```
