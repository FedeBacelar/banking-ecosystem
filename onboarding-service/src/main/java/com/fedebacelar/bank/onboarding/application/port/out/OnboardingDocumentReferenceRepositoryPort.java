package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingDocumentReferenceRepositoryPort {

    Optional<OnboardingDocumentReference> findByApplicationIdAndCategory(UUID applicationId, OnboardingDocumentCategory category);

    OnboardingDocumentReference save(OnboardingDocumentReference reference);
}
