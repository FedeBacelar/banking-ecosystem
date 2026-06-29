# Testing Strategy

The services follow a layered testing strategy.

## Domain Tests

Test business rules without Spring.

Examples:

```txt
Invalid lifecycle transitions.
Closed account restrictions.
Customer status rules.
```

## Use Case Tests

Test application behavior with mocked ports.

Examples:

```txt
Open account for active customer.
Reject account opening for non-active customer.
Reject duplicated account alias.
```

## Web Adapter Tests

Use MockMvc and mocked use cases.

Examples:

```txt
Valid request returns expected status.
Invalid body returns ProblemDetail.
Business conflict maps to 409.
```

## Persistence Integration Tests

Use Testcontainers with MySQL.

Examples:

```txt
Persist and reassemble aggregate.
Generate numbers from transactional sequence.
Verify Flyway migrations are valid.
```

## End-To-End Tests

Used where useful to validate HTTP flow with a real Spring context and database container.

## Current Commands

```powershell
cd customer-service
.\mvnw.cmd test
```

```powershell
cd account-service
.\mvnw.cmd test
```
