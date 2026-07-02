# config-server

`config-server` is the platform service that serves centralized configuration to the ecosystem.

## Runtime

```txt
Service name: config-server
Default port: 8888
Configuration source: config-repository
Mode: native
```

## Endpoints

```txt
GET /application/default
GET /eureka-server/default
GET /customer-service/default
GET /account-service/default
GET /identity-service/default
GET /api-gateway/default
```

## Local Startup Order

```txt
1. MySQL containers
2. config-server
3. eureka-server
4. customer-service
5. account-service
6. identity-service
7. api-gateway
```

`config-server` starts before Eureka because the other services read their operational configuration from it.

## Current Clients

```txt
eureka-server
customer-service
account-service
identity-service
api-gateway
```
