package com.fedebacelar.bank.document.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.enums.DocumentStorageProvider;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
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
        Instant updatedAt
) {
}
