package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import java.time.Instant;
import java.util.UUID;

public record DocumentReferenceResponse(
        UUID id,
        UUID applicationId,
        String category,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt
) {

    public OnboardingDocumentReference toDomain() {
        return new OnboardingDocumentReference(
                id,
                applicationId,
                category,
                documentId,
                createdAt,
                updatedAt
        );
    }
}
