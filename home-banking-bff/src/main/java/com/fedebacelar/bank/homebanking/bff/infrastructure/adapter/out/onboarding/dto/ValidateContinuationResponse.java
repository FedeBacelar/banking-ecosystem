package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import java.time.Instant;
import java.util.UUID;

public record ValidateContinuationResponse(
        UUID applicationId,
        String email,
        String status,
        Instant continuationExpiresAt
) {

    public OnboardingSession toSession() {
        return OnboardingSession.active(applicationId, status, continuationExpiresAt);
    }
}
