# document-service Tests

Run:

```powershell
cd document-service
.\mvnw.cmd test
```

Current coverage:

```txt
DocumentServiceTest
DocumentWebAdapterTest
MinioObjectStorageAdapterTest
DocumentServiceApplicationTests
```

The context test uses MySQL Testcontainers and validates the Flyway schema with Hibernate.

The first implementation does not run MinIO as a Testcontainer. Storage is isolated behind `ObjectStoragePort` for use case tests, the adapter verifies the S3 boundary with a mocked `S3Client`, and the real MinIO runtime is exercised when running the service locally with `infra/minio`.
