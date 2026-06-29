# eureka-server Implementation

`eureka-server` is the current service discovery component.

It is a platform service. It does not own banking business data.

## Current Status

Implemented.

Current capabilities:

- Runs a Netflix Eureka Server.
- Exposes the Eureka dashboard.
- Allows `customer-service` and `account-service` to register themselves.
- Allows service-to-service resolution by logical service name.

## Local Runtime

Default HTTP port:

```txt
8761
```

Dashboard:

```txt
http://localhost:8761
```

## Configuration

Main file:

```txt
eureka-server/src/main/resources/application.yaml
```

Important values:

```txt
spring.application.name=eureka-server
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

## Clients

Current Eureka clients:

```txt
customer-service
account-service
```

Both services use:

```txt
EUREKA_SERVER_URL=http://localhost:8761/eureka/
```

as the default local registry URL.

## Tests

Current test command:

```powershell
cd eureka-server
.\mvnw.cmd test
```

Current verified result:

```txt
1 test passing
```

## Local Startup Order

```txt
1. infra/mysql
2. eureka-server
3. customer-service
4. account-service
```
