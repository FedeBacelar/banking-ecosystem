# eureka-server

Service discovery server for the banking ecosystem.

This is a platform service, not a business service. It does not own banking data and does not expose customer/account operations.

## Responsibility

`eureka-server` keeps a registry of running service instances.

Current registered services:

```txt
customer-service
account-service
api-gateway
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

Start Config Server first:

```powershell
cd ..\config-server
.\mvnw.cmd spring-boot:run
```

Then start Eureka:

```powershell
cd ..\eureka-server
.\mvnw.cmd spring-boot:run
```

## Configuration

The service reads operational configuration from Config Server.

Config source:

```txt
../config-repository/eureka-server.yaml
```

Local bootstrap config remains in:

```txt
src/main/resources/application.yaml
```

The server does not register itself as a client.

## Startup Order

Recommended local order:

```txt
1. MySQL containers
2. config-server
3. eureka-server
4. customer-service
5. account-service
6. api-gateway
```

After startup, open:

```txt
http://localhost:8761
```

You should see `CUSTOMER-SERVICE`, `ACCOUNT-SERVICE`, and `API-GATEWAY` registered.
