package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingDocumentReference(
        UUID id,
        UUID applicationId,
        String category,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt
) {
}
