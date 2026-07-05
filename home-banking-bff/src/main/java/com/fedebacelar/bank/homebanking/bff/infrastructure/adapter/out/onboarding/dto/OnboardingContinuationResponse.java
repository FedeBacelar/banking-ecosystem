package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import java.time.Instant;
import java.util.UUID;

public record OnboardingContinuationResponse(
        UUID applicationId,
        String status,
        String continuationToken,
        Instant continuationExpiresAt
) {

    public OnboardingContinuation toContinuation() {
        return new OnboardingContinuation(applicationId, status, continuationToken, continuationExpiresAt);
    }

    public OnboardingSession toSession() {
        return OnboardingSession.active(applicationId, status, continuationExpiresAt);
    }
}
