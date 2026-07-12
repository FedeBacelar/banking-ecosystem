package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import java.util.UUID;

public interface DocumentValidationPort {
    boolean isStoredOnboardingDocument(UUID documentId, UUID applicationId, OnboardingDocumentCategory category);
}
