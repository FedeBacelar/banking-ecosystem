# customer-service API

This document lists the endpoints implemented today.

Base path:

```txt
/customers
```

## Registration

```http
POST /customers/natural-persons
```

Creates a natural person customer.

The optional `Idempotency-Key` header is validated (maximum 160 characters). Repeating the same key and payload returns the original customer. Reusing the key with a different payload returns `409 IDEMPOTENCY_CONFLICT`.

Current responsibilities:

- Create the party and natural person records.
- Create a bank customer record.
- Generate the customer number.
- Store one identification document.
- Store contacts and addresses.
- Create the initial KYC profile.
- Create the initial status history entry.

## Queries

```http
GET /customers/{customerId}
```

Returns a customer by internal id.

```http
GET /customers/by-document?type=DNI&number=12345678&country=AR
```

Returns a customer by document type, document number, and issuing country.

```http
GET /customers/by-number/{customerNumber}
```

Returns a customer by generated bank customer number.

```http
GET /customers/by-email?email=applicant@example.com
```

Uses normalized email contact data for onboarding duplicate checks.

```http
GET /customers/{customerId}/status-history
```

Returns the customer lifecycle history.

## KYC

```http
PATCH /customers/{customerId}/kyc/approve
PATCH /customers/{customerId}/kyc/reject
```

Updates the KYC result and adjusts the customer lifecycle according to the domain rules.

KYC approval requires an auditable body:

```json
{
  "reasonCode": "AUTO_ONBOARDING_APPROVED",
  "changedBy": "onboarding-service"
}
```

## Lifecycle

```http
PATCH /customers/{customerId}/suspend
PATCH /customers/{customerId}/reactivate
PATCH /customers/{customerId}/close
```

Changes the operational status of the customer.

Current statuses:

```txt
PENDING_KYC
ACTIVE
SUSPENDED
CLOSED
```

## Error Format

The service uses Spring `ProblemDetail` for API errors.

Examples:

- `400 Bad Request` for validation errors or malformed JSON.
- `404 Not Found` when a customer does not exist.
- `409 Conflict` for domain conflicts.

## Current Integration Consumers

`account-service` calls:

```http
GET /customers/{customerId}
```

It uses this endpoint to verify that a customer exists and is eligible before opening an account.
