package com.fedebacelar.bank.onboarding.application.view;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import java.time.Instant;
import java.util.UUID;

public record DocumentReferenceDetails(
        UUID id,
        UUID applicationId,
        OnboardingDocumentCategory category,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt
) {
}
