package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingPublicStatus(
        UUID applicationId,
        OnboardingState status,
        OnboardingNextAction nextAction,
        Instant updatedAt
) {
}
