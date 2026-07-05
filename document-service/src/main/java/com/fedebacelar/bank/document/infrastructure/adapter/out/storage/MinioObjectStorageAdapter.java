package com.fedebacelar.bank.document.infrastructure.adapter.out.storage;

import com.fedebacelar.bank.document.application.port.out.ObjectStoragePort;
import com.fedebacelar.bank.document.domain.exception.DocumentStorageException;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class MinioObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3Client;
    private final String bucketName;

    public MinioObjectStorageAdapter(
            S3Client s3Client,
            @Value("${document.storage.bucket}") String bucketName
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public StoredObject store(String objectKey, DocumentFile file) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.contentType())
                .contentLength(file.size())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.content(), file.size()));
            return new StoredObject(bucketName, objectKey);
        } catch (S3Exception exception) {
            throw new DocumentStorageException("Document storage failed: " + exception.awsErrorDetails().errorMessage(), exception);
        } catch (RuntimeException exception) {
            throw new DocumentStorageException("Document storage failed: " + exception.getMessage(), exception);
        }
    }
}

