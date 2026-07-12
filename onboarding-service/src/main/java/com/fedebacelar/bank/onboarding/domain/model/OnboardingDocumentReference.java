package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import java.time.Instant;
import java.util.UUID;

public record OnboardingDocumentReference(
        UUID id,
        UUID applicationId,
        OnboardingDocumentCategory category,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public static OnboardingDocumentReference create(
            UUID applicationId,
            OnboardingDocumentCategory category,
            UUID documentId,
            Instant now
    ) {
        return new OnboardingDocumentReference(
                UUID.randomUUID(),
                applicationId,
                category,
                documentId,
                now,
                now,
                0L
        );
    }

    public OnboardingDocumentReference updateDocument(UUID documentId, Instant now) {
        return new OnboardingDocumentReference(
                id,
                applicationId,
                category,
                documentId,
                createdAt,
                now,
                version
        );
    }
}
