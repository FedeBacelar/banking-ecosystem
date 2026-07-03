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
home-banking-bff
config-server
eureka-server
api-gateway
```

## Current Infrastructure

```txt
MySQL local with one database container per business service.
Config Server for centralized configuration.
Eureka Server for service discovery.
API Gateway for external HTTP routing.
Keycloak for local identity provider infrastructure.
Banking BFF for browser sessions through the gateway.
```
