package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingTermsAcceptanceRepositoryPort {

    Optional<OnboardingTermsAcceptance> findByApplicationId(UUID applicationId);

    OnboardingTermsAcceptance save(OnboardingTermsAcceptance acceptance);
}
