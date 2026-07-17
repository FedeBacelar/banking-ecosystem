# document-service Database

Database:

```txt
document_db
```

## Tables

```txt
document
```

## document

Stores document metadata and the object storage location.

Important columns:

```txt
id
idempotency_key
content_sha256
business_context
business_reference_id
category
original_filename
content_type
size_bytes
storage_provider
bucket_name
object_key
status
created_at
updated_at
version
```

Indexes:

```txt
uk_document_object_key
uk_document_idempotency_key
idx_document_content_sha256
idx_document_business_reference
idx_document_category
idx_document_status
idx_document_created_at
```

`idempotency_key` prevents duplicate metadata rows when an upload is retried, while
`content_sha256` records the uploaded content fingerprint. Both columns are nullable
for compatibility with documents created before Flyway migration `V2`.

Flyway migrations `V1` through `V2` create the current schema.
