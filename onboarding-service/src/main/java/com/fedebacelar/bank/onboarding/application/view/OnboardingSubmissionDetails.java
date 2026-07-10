package com.fedebacelar.bank.onboarding.application.view;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record OnboardingSubmissionDetails(
        UUID applicationId,
        OnboardingApplicationStatus status,
        Instant submittedAt,
        Instant updatedAt
) {
}
