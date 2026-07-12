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
idx_document_business_reference
idx_document_category
idx_document_status
idx_document_created_at
```
