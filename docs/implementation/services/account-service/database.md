# account-service Database

`account-service` owns its own database.

Local database:

```txt
account_db
```

Local host port:

```txt
3308
```

Container internal port:

```txt
3306
```

## Migrations

```txt
V1__create_account_schema.sql
V2__add_account_lookup_indexes.sql
V3__create_account_number_sequence.sql
V4__create_account_idempotency.sql
```

## Tables

### account

Represents a bank account.

Important fields:

- `customer_id`: id of the customer in `customer-service`.
- `account_number`: generated account number.
- `cbu`: generated CBU.
- `alias`: optional human-friendly identifier.
- `type`: account type.
- `currency`: account currency.
- `status`: account lifecycle status.
- `opened_at`.
- `closed_at`.
- `version`: optimistic locking column.

Unique identifiers:

- `account_number`.
- `cbu`.
- `alias`.

### account_balance

Stores the current operational balance for an account.

Important fields:

- `available_balance`.
- `current_balance`.
- `currency`.
- `updated_at`.
- `version`.

Today this table is initialized at account opening time. Money movement operations are outside the current implementation.

### account_status_history

Stores lifecycle changes.

It lets us know when and why an account moved between statuses.

### account_number_sequence

Stores the sequence used to generate account numbers.

### account_idempotency

Stores idempotency key, request hash, created account, and timestamp in the account-opening transaction.

## Ownership Rule

No other service should write to this database.

Other services must consume account information through the account API or explicit integration mechanisms.

