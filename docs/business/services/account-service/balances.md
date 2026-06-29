# Account Balances

`account-service` models account balance as an operational position, not as a complete transaction history.

## Current Fields

```txt
currentBalance
availableBalance
holdBalance
```

## currentBalance

The total accounting position of the account.

## availableBalance

The amount currently available for account operations.

## holdBalance

The amount that is reserved, retained, or not currently available.

## Current Behavior

When an account is opened, all balances start at zero.

The current implementation does not support deposits, withdrawals, transfers, or holds.
