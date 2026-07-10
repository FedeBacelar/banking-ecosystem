package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import java.time.Instant;
import java.util.UUID;

public record OnboardingDocumentReferenceResponse(
        UUID id,
        UUID applicationId,
        String category,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt
) {

    public static OnboardingDocumentReferenceResponse from(OnboardingDocumentReference reference) {
        return new OnboardingDocumentReferenceResponse(
                reference.id(),
                reference.applicationId(),
                reference.category(),
                reference.documentId(),
                reference.createdAt(),
                reference.updatedAt()
        );
    }
}
