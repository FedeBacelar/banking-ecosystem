# Database Per Service

Each service owns its database.

This avoids accidental coupling and keeps each service responsible for its own data model.

## Current Local Setup

```txt
customer-service -> customer_db
account-service -> account_db
```

Each database runs in its own MySQL container for local development.

## No Cross-Database Foreign Keys

`account-service` stores `customerId`, but it does not create a foreign key to `customer_db`.

Reason:

```txt
customer-service owns customer data.
account-service owns account data.
```

The relationship is validated through service communication, not shared database constraints.

## Migrations

Each service owns its Flyway migrations:

```txt
customer-service/src/main/resources/db/migration
account-service/src/main/resources/db/migration
```

## Production Direction

The production concept should remain:

```txt
One logical database per service.
No shared tables.
No service reading another service's database directly.
```
