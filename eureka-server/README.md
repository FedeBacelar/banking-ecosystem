# eureka-server

Service discovery server for the banking ecosystem.

This is a platform service, not a business service. It does not own banking data and does not expose customer/account operations.

## Responsibility

`eureka-server` keeps a registry of running service instances.

Current registered services:

```txt
customer-service
account-service
```

## Local Runtime

Default port:

```txt
8761
```

Dashboard:

```txt
http://localhost:8761
```

## Run

```powershell
cd eureka-server
.\mvnw.cmd spring-boot:run
```

## Configuration

Main config file:

```txt
src/main/resources/application.yaml
```

Important values:

```txt
spring.application.name=eureka-server
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

The server does not register itself as a client.

## Startup Order

Recommended local order:

```txt
1. MySQL containers
2. eureka-server
3. customer-service
4. account-service
```

After startup, open:

```txt
http://localhost:8761
```

You should see `CUSTOMER-SERVICE` and `ACCOUNT-SERVICE` registered.
