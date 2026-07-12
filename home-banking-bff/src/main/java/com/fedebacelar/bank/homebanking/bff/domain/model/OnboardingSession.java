package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OnboardingSession(
        boolean active,
        UUID applicationId,
        OnboardingState status,
        Instant continuationExpiresAt,
        Instant updatedAt
) {

    public static OnboardingSession anonymous() {
        return new OnboardingSession(false, null, null, null, null);
    }

    public static OnboardingSession active(UUID applicationId, OnboardingState status, Instant continuationExpiresAt) {
        return active(applicationId, status, continuationExpiresAt, null);
    }

    public static OnboardingSession active(UUID applicationId, OnboardingState status, Instant continuationExpiresAt, Instant updatedAt) {
        return new OnboardingSession(
                true,
                Objects.requireNonNull(applicationId, "applicationId is required for an active onboarding session"),
                status,
                continuationExpiresAt,
                updatedAt
        );
    }
}
