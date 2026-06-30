# account-service Implementation

`account-service` is the current account bounded context.

It owns bank accounts, account identifiers, account status, operational balances, and account status history. It validates the customer through `customer-service` before opening an account.

## Current Status

Functional current implementation.

Implemented:

- Open an account for an existing eligible customer.
- Generate account number and CBU.
- Store and query account alias.
- Store account type, currency, and status.
- Initialize account balance at zero.
- Query account, balance, and status history.
- Activate, freeze, unfreeze, and close accounts.
- Validate customer existence and status through Feign.
- Configure bank and branch identifier values through properties.
- Validate request sizes against database limits.
- Return consistent `ProblemDetail` errors.
- Run unit, web adapter, and persistence tests.

## Local Runtime

Operational configuration is served by config-server from config-repository/account-service.yaml.


Default HTTP port:

```txt
8081
```

Swagger:

```txt
http://localhost:8081/swagger-ui.html
```

Database:

```txt
account_db on localhost:3308
```

## Related Docs

- [API](api.md)
- [Database](database.md)
- [Tests](tests.md)
- [Customer integration](customer-integration.md)
- Business docs: [account-service](../../../business/services/account-service/README.md)

