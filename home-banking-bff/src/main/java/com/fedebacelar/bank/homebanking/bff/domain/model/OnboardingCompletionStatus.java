package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;

public record OnboardingCompletionStatus(
        OnboardingCompletionState status,
        Instant updatedAt
) {
}
