# document-service API

## Upload Document

```txt
POST /internal/documents
Content-Type: multipart/form-data
```

Form fields:

```txt
businessContext=ONBOARDING_APPLICATION
businessReferenceId=11111111-1111-1111-1111-111111111111
category=DNI_FRONT
file=@dni-front.jpg
```

Allowed categories:

```txt
DNI_FRONT
DNI_BACK
```

Allowed content types:

```txt
image/jpeg
image/png
application/pdf
```

Response status:

```txt
201 Created
```

Response:

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "businessContext": "ONBOARDING_APPLICATION",
  "businessReferenceId": "11111111-1111-1111-1111-111111111111",
  "category": "DNI_FRONT",
  "originalFilename": "dni-front.jpg",
  "contentType": "image/jpeg",
  "sizeBytes": 123456,
  "storageProvider": "MINIO",
  "bucketName": "banking-documents",
  "objectKey": "onboarding_application/11111111-1111-1111-1111-111111111111/DNI_FRONT/11111111-1111-1111-1111-111111111111",
  "status": "STORED",
  "createdAt": "2026-07-04T21:00:00Z",
  "updatedAt": "2026-07-04T21:00:00Z"
}
```

## Get Document Metadata

```txt
GET /internal/documents/{documentId}
```

Response status:

```txt
200 OK
```

The response contains metadata only. File download is intentionally outside the first implementation.

## Errors

```txt
400 Invalid request or invalid document
404 Document not found
409 Concurrent document update
503 Document storage unavailable
```
