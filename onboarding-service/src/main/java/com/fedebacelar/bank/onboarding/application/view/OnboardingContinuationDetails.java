package com.fedebacelar.bank.onboarding.application.view;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record OnboardingContinuationDetails(
        UUID applicationId,
        String email,
        OnboardingApplicationStatus status,
        String continuationToken,
        Instant continuationExpiresAt
) {
}
