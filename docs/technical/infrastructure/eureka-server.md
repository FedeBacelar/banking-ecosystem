# Eureka Server

Eureka Server is implemented as:

```txt
eureka-server/
```

It provides service discovery for the banking ecosystem.

## Concept

In a distributed system, services need to find each other.

Without discovery, a service usually calls another service through a fixed URL:

```txt
http://localhost:8080
```

With discovery, a service calls another service by logical name:

```txt
customer-service
```

Eureka keeps a registry of service instances. Each service registers itself on startup and periodically renews its registration.

## Vocabulary

### Discovery Service

Generic name for the component that lets services discover each other.

Eureka is one implementation of this idea.

### Service Registry

The registry is the list of known service instances.

Example:

```txt
customer-service -> localhost:8080
account-service  -> localhost:8081
identity-service -> localhost:8082
home-banking-bff     -> localhost:8086
api-gateway      -> localhost:8085
```

### Eureka Server

The server that stores and exposes the registry.

### Eureka Client

Each application that registers itself in Eureka.

Current clients:

```txt
customer-service
account-service
identity-service
document-service
notification-service
onboarding-service
home-banking-bff
api-gateway
```

### API Gateway

A gateway is different from Eureka.

Eureka helps services find each other. A gateway gives external clients a single entry point.

Current gateway responsibility:

```txt
/web/** -> home-banking-bff
```

Business services discover each other through Eureka and do not become external gateway routes.

### Config Server

Config Server is also different.

Eureka answers:

```txt
Where is this service running?
```

Config Server answers:

```txt
What configuration should this service use?
```

## Current Configuration

Eureka Server:

```txt
http://localhost:8761
```

Client registration URL:

```txt
http://localhost:8761/eureka/
```

## Current Integration

`customer-service` registers itself in Eureka.

`account-service` registers itself in Eureka and consumes `customer-service` by service name through Feign:

```java
@FeignClient(name = "customer-service")
```

`api-gateway` registers itself in Eureka and routes external HTTP requests to business services by logical service name.

`identity-service` registers itself in Eureka so internal components can resolve identity links by service name.

`home-banking-bff` registers itself in Eureka so `api-gateway` can route `/web/**` traffic to it by service name.

## Local Startup Order

```txt
1. Start MySQL containers.
2. Start config-server.
3. Start eureka-server.
4. Start customer-service.
5. Start account-service.
6. Start identity-service.
7. Start home-banking-bff.
8. Start api-gateway.
```

## Why It Belongs Here

We now have more than one business service and at least one service-to-service call:

```txt
account-service -> customer-service
```

That makes service discovery useful and justified.

