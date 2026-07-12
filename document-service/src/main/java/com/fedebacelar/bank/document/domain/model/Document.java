package com.fedebacelar.bank.document.domain.model;

import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.enums.DocumentStorageProvider;
import java.time.Instant;
import java.util.UUID;

public record Document(
        UUID id,
        String idempotencyKey,
        String contentSha256,
        String businessContext,
        String businessReferenceId,
        DocumentCategory category,
        String originalFilename,
        String contentType,
        long sizeBytes,
        DocumentStorageProvider storageProvider,
        String bucketName,
        String objectKey,
        DocumentStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public static Document pending(
            UUID id,
            String idempotencyKey,
            String contentSha256,
            String businessContext,
            String businessReferenceId,
            DocumentCategory category,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String bucketName,
            String objectKey,
            Instant now
    ) {
        return new Document(
                id,
                idempotencyKey,
                contentSha256,
                businessContext,
                businessReferenceId,
                category,
                originalFilename,
                contentType,
                sizeBytes,
                DocumentStorageProvider.MINIO,
                bucketName,
                objectKey,
                DocumentStatus.PENDING,
                now,
                now,
                0L
        );
    }

    public Document markStored(Instant now) {
        return withStatus(DocumentStatus.STORED, now);
    }

    public Document markFailed(Instant now) {
        return withStatus(DocumentStatus.FAILED, now);
    }

    private Document withStatus(DocumentStatus nextStatus, Instant now) {
        return new Document(
                id,
                idempotencyKey,
                contentSha256,
                businessContext,
                businessReferenceId,
                category,
                originalFilename,
                contentType,
                sizeBytes,
                storageProvider,
                bucketName,
                objectKey,
                nextStatus,
                createdAt,
                now,
                version
        );
    }
}
