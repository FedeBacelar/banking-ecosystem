# Project Documentation

This directory is the public knowledge base for the current banking ecosystem implementation.

The documentation is organized in four sections:

```txt
business
technical
implementation
database
```

## Business

Business documentation explains what each current concept means in the banking domain.

Start here:

```txt
docs/business/README.md
```

## Technical

Technical documentation explains engineering decisions and conventions used by the current codebase.

Start here:

```txt
docs/technical/README.md
```

## Implementation

Implementation documentation explains what exists in the repository today.

Start here:

```txt
docs/implementation/README.md
```

## Database

Database documentation explains the current physical model from service migrations and keeps the editable ERD source.

Start here:

```txt
docs/database/README.md
```

## Current Services

```txt
customer-service
account-service
identity-service
notification-service
document-service
onboarding-service
home-banking-bff
config-server
eureka-server
api-gateway
banking-web
```

## Current Infrastructure

```txt
MySQL with one local database container per stateful business service.
Config Server for centralized configuration.
Eureka Server for service discovery.
API Gateway for external HTTP routing.
Keycloak for human and machine identity in the local environment.
MinIO for document object storage.
Mailpit for local SMTP capture and email inspection.
Home Banking BFF for browser sessions and public web contracts through the gateway.
OpenTelemetry Collector, Prometheus, Loki, Tempo, and Grafana through the pinned local LGTM development stack.
```
