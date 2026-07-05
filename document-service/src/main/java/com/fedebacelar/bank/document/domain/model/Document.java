package com.fedebacelar.bank.document.domain.model;

import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.enums.DocumentStorageProvider;
import java.time.Instant;
import java.util.UUID;

public record Document(
        UUID id,
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

    public static Document createStored(
            String businessContext,
            String businessReferenceId,
            DocumentCategory category,
            String originalFilename,
            String contentType,
            long sizeBytes,
            DocumentStorageProvider storageProvider,
            String bucketName,
            String objectKey,
            Instant now
    ) {
        return new Document(
                UUID.randomUUID(),
                businessContext,
                businessReferenceId,
                category,
                originalFilename,
                contentType,
                sizeBytes,
                storageProvider,
                bucketName,
                objectKey,
                DocumentStatus.STORED,
                now,
                now,
                0L
        );
    }
}

