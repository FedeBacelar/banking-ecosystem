package com.fedebacelar.bank.document.infrastructure.adapter.out.storage;

import com.fedebacelar.bank.document.application.port.out.ObjectStoragePort;
import com.fedebacelar.bank.document.domain.exception.DocumentStorageException;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream content = new DigestInputStream(file.openStream(), digest)) {
                s3Client.putObject(request, RequestBody.fromInputStream(content, file.size()));
            }
            return new StoredObject(bucketName, objectKey, HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException exception) {
            throw new DocumentStorageException("SHA-256 digest is unavailable", exception);
        } catch (java.io.IOException exception) {
            throw new DocumentStorageException("Could not read document content", exception);
        } catch (S3Exception exception) {
            throw new DocumentStorageException("Document storage failed: " + exception.awsErrorDetails().errorMessage(), exception);
        } catch (RuntimeException exception) {
            throw new DocumentStorageException("Document storage failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (RuntimeException exception) {
            throw new DocumentStorageException("Document cleanup failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public String bucketName() {
        return bucketName;
    }
}

