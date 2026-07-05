# document-service

Document metadata and object storage integration for banking files and evidence.

The service owns document metadata. File bytes are stored in S3-compatible object storage, locally MinIO, using keys such as:

```txt
onboarding/{applicationId}/{category}/{documentId}
```

## Responsibilities

- Validate allowed document content types and size.
- Store uploaded files in MinIO.
- Persist document metadata in `document_db`.
- Expose internal APIs for upload and metadata lookup.

It does not own onboarding decisions, document review, customer data, or public download/viewer flows.

## Local Requirements

Start local dependencies from the repository root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d document-mysql
docker compose -f infra/minio/docker-compose.yml up -d
```

Start platform services:

```powershell
cd config-server
.\mvnw.cmd spring-boot:run
```

```powershell
cd ..\eureka-server
.\mvnw.cmd spring-boot:run
```

Run the service:

```powershell
cd ..\document-service
.\mvnw.cmd spring-boot:run
```

Swagger:

```txt
http://localhost:8084/swagger-ui.html
```

## API

Upload:

```http
POST /internal/documents
Content-Type: multipart/form-data
```

Required form fields:

- `businessContext`: uppercase context, initially `ONBOARDING`.
- `businessReferenceId`: owning business reference, for onboarding this is the application id.
- `category`: `DNI_FRONT` or `DNI_BACK`.
- `file`: JPG, PNG, or PDF up to 10 MB by default.

Metadata:

```http
GET /internal/documents/{documentId}
```

Security:

- `DOCUMENT_WRITE` is required for upload.
- `DOCUMENT_READ` is required for metadata lookup.

## Configuration

Runtime configuration comes from `config-repository/document-service.yaml`.

Use `.env.example` as a local reference only. Do not commit `.env` files.

## Tests

```powershell
.\mvnw.cmd test
```
