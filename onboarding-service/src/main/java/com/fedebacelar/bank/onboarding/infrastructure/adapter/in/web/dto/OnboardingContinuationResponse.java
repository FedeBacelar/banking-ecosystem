package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record OnboardingContinuationResponse(
        UUID applicationId,
        String email,
        OnboardingApplicationStatus status,
        String continuationToken,
        Instant continuationExpiresAt
) {
}
