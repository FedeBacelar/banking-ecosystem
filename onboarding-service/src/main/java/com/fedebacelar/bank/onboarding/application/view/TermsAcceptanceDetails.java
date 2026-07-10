package com.fedebacelar.bank.onboarding.application.view;

import java.time.Instant;
import java.util.UUID;

public record TermsAcceptanceDetails(
        UUID applicationId,
        String termsVersion,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
