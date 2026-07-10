# account-service API

This document lists the endpoints implemented today.

Base path:

```txt
/accounts
```

## Opening

```http
POST /accounts
```

Creates a bank account.

The optional `Idempotency-Key` header follows the same durable contract as customer creation: same key/payload returns the original account; payload drift returns `409 IDEMPOTENCY_CONFLICT`.

Current responsibilities:

- Validate that the customer exists through `customer-service`.
- Validate that the customer is eligible to open an account.
- Generate an account number.
- Generate a CBU.
- Optionally store an alias.
- Store type, currency, and initial status.
- Create an account balance initialized at zero.
- Create the initial status history entry.

## Queries

```http
GET /accounts/{accountId}
```

Returns an account by internal id.

```http
GET /accounts/by-number/{accountNumber}
```

Returns an account by generated account number.

```http
GET /accounts/by-alias/{alias}
```

Returns an account by alias.

```http
GET /accounts/customer/{customerId}
```

Returns accounts for a customer.

```http
GET /accounts/{accountId}/balance
```

Returns the current operational balance.

```http
GET /accounts/{accountId}/status-history
```

Returns account lifecycle history.

## Alias

```http
PATCH /accounts/{accountId}/alias
```

Updates the account alias.

Current alias validation:

- Maximum 80 characters.
- Lowercase letters and numbers.
- Dot or hyphen as segment separators.
- Between 2 and 6 alias segments.

Example:

```txt
fedeb.bank.ars
```

## Lifecycle

```http
PATCH /accounts/{accountId}/activate
PATCH /accounts/{accountId}/freeze
PATCH /accounts/{accountId}/unfreeze
PATCH /accounts/{accountId}/close
```

Current account statuses:

```txt
PENDING_ACTIVATION
ACTIVE
FROZEN
CLOSED
```

## Supported Account Types

```txt
SAVINGS
CHECKING
SALARY
```

## Supported Currencies

```txt
ARS
USD
```

Currency conversion is not implemented in this service.
