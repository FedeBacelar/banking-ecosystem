package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import java.time.Instant;
import java.util.UUID;

public record ValidateContinuationResponse(
        UUID applicationId,
        String email,
        OnboardingState status,
        Instant continuationExpiresAt,
        Instant updatedAt
) {

    public OnboardingSession toSession() {
        return OnboardingSession.active(applicationId, status, continuationExpiresAt, updatedAt);
    }
}
