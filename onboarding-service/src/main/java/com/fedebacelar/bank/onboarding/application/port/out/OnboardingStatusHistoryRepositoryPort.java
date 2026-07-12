package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;

public interface OnboardingStatusHistoryRepositoryPort {
    OnboardingStatusHistory save(OnboardingStatusHistory history);
}
