package com.fedebacelar.bank.onboarding.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingTermsAcceptance(
        UUID applicationId,
        String termsVersion,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public static OnboardingTermsAcceptance accept(UUID applicationId, String termsVersion, Instant now) {
        return new OnboardingTermsAcceptance(
                applicationId,
                termsVersion,
                now,
                now,
                now,
                0L
        );
    }
}
