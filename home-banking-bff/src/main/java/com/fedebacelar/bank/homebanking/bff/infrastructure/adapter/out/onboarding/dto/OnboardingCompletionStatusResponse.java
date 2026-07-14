package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubjectStatus;
import java.time.Instant;

public record OnboardingCompletionStatusResponse(
        OnboardingState status,
        Instant updatedAt
) {
    public OnboardingSubjectStatus toDomain() {
        return new OnboardingSubjectStatus(status, updatedAt);
    }
}
