package com.fedebacelar.bank.onboarding.application.command;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import java.util.UUID;

public record SaveDocumentReferenceCommand(
        String continuationToken,
        OnboardingDocumentCategory category,
        UUID documentId
) {
}
