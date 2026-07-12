# Hexagonal Architecture

The services use a hexagonal architecture style.

The goal is to keep business logic independent from HTTP, JPA, MySQL, Feign, and other infrastructure details.

## Current Package Style

```txt
domain
application
infrastructure
```

## Domain

Contains business concepts:

```txt
model
enums
exception
```

Domain classes should not depend on Spring MVC, JPA, Feign, or database classes.

## Application

Contains use cases and ports.

Current convention:

```txt
application/
  command/
  mapper/
  port/
    in/
    out/
  usecase/
    {business-capability}/
  view/
```

Use cases are grouped by business capability.

Ports are kept under `in` and `out`; larger services group use cases by business capability without changing the dependency direction.

## Infrastructure

Contains adapters and framework code.

Examples:

```txt
adapter/in/web
adapter/out/persistence
adapter/out/customer
config
```

Adapters translate between external technologies and application ports.

## Important Rule

```txt
HTTP DTO != Application Command != Domain Model != JPA Entity
```

Feign DTOs should not leak into use cases.
