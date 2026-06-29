# Account Lifecycle

The account lifecycle describes the operational state of a bank account.

## Current States

```txt
PENDING_ACTIVATION
ACTIVE
FROZEN
CLOSED
```

## PENDING_ACTIVATION

The account was opened but is not active yet.

This allows account opening to be separated from account activation.

## ACTIVE

The account is operational.

The current implementation does not expose debit or credit operations.

## FROZEN

The account is temporarily restricted.

This may represent fraud review, compliance review, legal restriction, or internal operational control.

In the current implementation, freezing is a lifecycle operation. There are no financial operations yet.

## CLOSED

The account is closed.

This is terminal in the current model.

## Allowed Transitions

```txt
PENDING_ACTIVATION -> ACTIVE
PENDING_ACTIVATION -> CLOSED
ACTIVE -> FROZEN
ACTIVE -> CLOSED
FROZEN -> ACTIVE
FROZEN -> CLOSED
```

There is no generic endpoint to freely set account status. Status changes happen through explicit business actions.

## Closing Rule

An account cannot be closed if its balance is not zero.

