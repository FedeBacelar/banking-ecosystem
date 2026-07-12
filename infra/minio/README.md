# Local MinIO

Local S3-compatible object storage behind the generic `document-service` boundary.

The included credentials are development defaults. To override them, create a local `.env` file from `.env.example`. Local `.env` files must not be committed.

## Responsibility

MinIO provides local object storage.

It does not own document metadata or business rules. Those responsibilities belong to `document-service`.

## Start

From the repository root:

```powershell
docker compose -f infra/minio/docker-compose.yml up -d
```

With custom local variables:

```powershell
docker compose --env-file infra/minio/.env -f infra/minio/docker-compose.yml up -d
```

The compose file starts MinIO and runs a one-shot bucket initialization container.

## Stop

```powershell
docker compose -f infra/minio/docker-compose.yml down
```

To remove local stored objects:

```powershell
docker compose -f infra/minio/docker-compose.yml down -v
```

## Local Defaults

API:

```txt
http://localhost:9000
```

Console:

```txt
http://localhost:9001
```

Credentials:

```txt
Username: nerva_minio
Password: nerva_minio_password
```

Bucket:

```txt
banking-documents
```

## document-service Configuration

```txt
DOCUMENT_STORAGE_PROVIDER=minio
DOCUMENT_STORAGE_ENDPOINT=http://localhost:9000
DOCUMENT_STORAGE_BUCKET=banking-documents
DOCUMENT_STORAGE_ACCESS_KEY=nerva_minio
DOCUMENT_STORAGE_SECRET_KEY=nerva_minio_password
DOCUMENT_STORAGE_REGION=us-east-1
DOCUMENT_STORAGE_PATH_STYLE_ACCESS=true
```

Do not commit real storage credentials.

