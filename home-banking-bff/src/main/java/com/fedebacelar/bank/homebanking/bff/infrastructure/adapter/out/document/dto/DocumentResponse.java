package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.document.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocument;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String businessContext,
        String businessReferenceId,
        String category,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String storageProvider,
        String bucketName,
        String objectKey,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public OnboardingDocument toDomain() {
        return new OnboardingDocument(
                id,
                businessContext,
                businessReferenceId,
                category,
                originalFilename,
                contentType,
                sizeBytes,
                status,
                createdAt,
                updatedAt
        );
    }
}
