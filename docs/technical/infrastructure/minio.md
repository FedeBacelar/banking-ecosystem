# MinIO

MinIO provides local S3-compatible object storage for the banking ecosystem.

## Current Purpose

The first intended use case is document evidence storage for onboarding.

The storage responsibility is split:

```txt
document-service -> owns document metadata and access rules
MinIO            -> stores file content
```

Business services should not write directly to MinIO.

## Local Infrastructure

MinIO is defined in:

```txt
infra/minio/docker-compose.yml
```

Local endpoints:

```txt
API:     http://localhost:9000
Console: http://localhost:9001
```

Default bucket:

```txt
banking-documents
```

The Docker Compose setup includes a one-shot bucket initialization container.

## Local Development Defaults

```txt
MINIO_ROOT_USER=nerva_minio
MINIO_ROOT_PASSWORD=nerva_minio_password
MINIO_BUCKET_NAME=banking-documents
```

These are development defaults only. Real credentials must be injected from outside the repository.

## Service Configuration

`document-service` consumes MinIO through a storage port/adapter.

Local configuration:

```txt
DOCUMENT_STORAGE_PROVIDER=minio
DOCUMENT_STORAGE_ENDPOINT=http://localhost:9000
DOCUMENT_STORAGE_BUCKET=banking-documents
DOCUMENT_STORAGE_ACCESS_KEY=nerva_minio
DOCUMENT_STORAGE_SECRET_KEY=nerva_minio_password
DOCUMENT_STORAGE_REGION=us-east-1
DOCUMENT_STORAGE_PATH_STYLE_ACCESS=true
```

