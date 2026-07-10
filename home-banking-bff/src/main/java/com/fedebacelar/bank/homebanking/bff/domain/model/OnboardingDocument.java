package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingDocument(
        UUID id,
        String businessContext,
        String businessReferenceId,
        String category,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
