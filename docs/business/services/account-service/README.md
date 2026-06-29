# Account Service Business View

`account-service` represents bank accounts.

It is responsible for account identity, lifecycle, ownership reference, currency, and operational balance position.

## Owns

```txt
- Bank accounts.
- Account number.
- CBU.
- Alias.
- Account type.
- Account currency.
- Account status.
- Operational balances.
- Account status history.
```

## Does Not Own

```txt
- Personal customer data.
- KYC.
- Exchange rates.
- Deposits, withdrawals, or transfers as final transactions.
- Card issuing.
- Loans.
- Authentication users.
```

## Current Scope

The current implementation supports account creation, queries, alias changes, lifecycle state changes, and balance consultation.

Balances are initialized at zero. Money movement operations are outside the current implementation.

## Related Documents

```txt
lifecycle.md
account-types.md
balances.md
identifiers.md
```
