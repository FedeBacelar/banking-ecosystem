package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingReviewCheck;
import java.util.List;
import java.util.UUID;

public interface OnboardingReviewCheckRepositoryPort {
    OnboardingReviewCheck save(OnboardingReviewCheck check);
    List<OnboardingReviewCheck> findByApplicationId(UUID applicationId);
}
