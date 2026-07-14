package com.fedebacelar.bank.onboarding.application.view;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;

public record OnboardingCompletionDetails(
        OnboardingApplicationStatus status,
        Instant updatedAt
) {
}
