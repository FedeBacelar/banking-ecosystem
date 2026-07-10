package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingTermsAcceptance(
        UUID applicationId,
        String termsVersion,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
