# identity-service Database

`identity-service` owns `identity_db`.

## Tables

```txt
identity_link
```

## identity_link

Stores the link between an external authenticated identity and an internal banking customer.

Columns:

```txt
id
customer_id
provider
provider_subject
status
created_at
updated_at
version
```

Constraints:

```txt
PRIMARY KEY (id)
UNIQUE (provider, provider_subject)
```

Indexes:

```txt
idx_identity_link_customer_id
idx_identity_link_status
```

`customer_id` is a logical reference to `customer-service`.

There is no physical foreign key because each service owns its own database.
