package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingContinuation(
        UUID applicationId,
        OnboardingState status,
        String continuationToken,
        Instant continuationExpiresAt
) {
}
