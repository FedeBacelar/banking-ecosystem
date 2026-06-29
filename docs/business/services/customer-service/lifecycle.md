# Customer Lifecycle

The customer lifecycle describes the operational state of a customer in the bank.

## Current States

```txt
PENDING_KYC
ACTIVE
SUSPENDED
CLOSED
```

## PENDING_KYC

The customer was registered but has not completed KYC approval.

In this state, the customer is known by the bank but should not be treated as fully operational.

## ACTIVE

The customer passed KYC and is enabled for banking operations.

`account-service` currently requires this state before opening an account.

## SUSPENDED

The customer is temporarily restricted.

This may represent operational review, risk alert, compliance concerns, or other internal reasons.

## CLOSED

The customer relationship with the bank is closed.

This is terminal in the current model.

## Allowed Transitions

```txt
PENDING_KYC -> ACTIVE
PENDING_KYC -> CLOSED
ACTIVE -> SUSPENDED
ACTIVE -> CLOSED
SUSPENDED -> ACTIVE
SUSPENDED -> CLOSED
```

There is no generic endpoint to freely set a customer status. Status changes happen through explicit business actions.
