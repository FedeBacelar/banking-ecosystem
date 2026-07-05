package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingSession(
        boolean active,
        UUID applicationId,
        String status,
        Instant continuationExpiresAt
) {

    public static OnboardingSession anonymous() {
        return new OnboardingSession(false, null, null, null);
    }

    public static OnboardingSession active(UUID applicationId, String status, Instant continuationExpiresAt) {
        return new OnboardingSession(true, applicationId, status, continuationExpiresAt);
    }
}
