package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;

public record OnboardingSubjectStatus(
        OnboardingState status,
        Instant updatedAt
) {
}
