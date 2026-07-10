package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import java.time.Instant;
import java.util.UUID;

public record DocumentReferenceResponse(
        UUID id,
        UUID applicationId,
        OnboardingDocumentCategory category,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt
) {
}
