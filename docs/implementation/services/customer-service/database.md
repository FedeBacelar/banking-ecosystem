# customer-service Database

`customer-service` owns its own database.

Local database:

```txt
customer_db
```

Local host port:

```txt
3307
```

Container internal port:

```txt
3306
```

## Migrations

```txt
V1__create_customer_schema.sql
V2__add_customer_lookup_indexes.sql
V3__create_customer_number_sequence.sql
V4__add_customer_optimistic_lock_version.sql
V5__add_customer_email_lookup_index.sql
V6__create_customer_idempotency.sql
V7__add_customer_status_actor.sql
V8__add_canonical_document_number.sql
V9__reserve_customer_idempotency_before_creation.sql
```

## Tables

### party

Represents the person or organization behind a bank customer.

Current party types:

```txt
NATURAL_PERSON
LEGAL_ENTITY
```

Only natural person onboarding is implemented today.

### natural_person

Stores natural person data.

Examples:

- First name.
- Middle name.
- Last name.
- Birth date.

### customer

Represents the formal customer relationship with the bank.

Important fields:

- `customer_number`: bank-generated customer identifier.
- `status`: lifecycle status.
- `opened_at`.
- `closed_at`.
- `version`: optimistic locking column.

### identification_document

Stores customer identification documents.

Current document types:

```txt
DNI
PASSPORT
CUIT
```

The current unique constraint prevents duplicated documents by type, number, and issuing country.

### contact_point

Stores contact channels.

Current contact types:

```txt
EMAIL
PHONE
```

### address

Stores customer addresses.

Current address types:

```txt
HOME
LEGAL
```

### kyc_profile

Stores KYC review status for the customer.

Current KYC statuses:

```txt
PENDING_REVIEW
APPROVED
REJECTED
```

### customer_status_history

Stores lifecycle changes.

This is important for traceability: it lets us answer when and why a customer moved from one state to another.

The history includes `changed_by`; automated onboarding records `AUTO_ONBOARDING_APPROVED` with actor `onboarding-service`.

### customer_number_sequence

Stores the sequence used to generate bank customer numbers.

### customer_idempotency

Reserves the idempotency key and request hash before customer creation, then assigns the created customer in the same transaction. A row-level lock serializes concurrent requests using the same key; payload drift returns a conflict.

## Ownership Rule

No other service should write to this database.

Other services must consume customer information through the customer API or explicit integration mechanisms.

