package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionStatus;
import java.time.Instant;

public record OnboardingCompletionStatusResponse(
        OnboardingCompletionState status,
        Instant updatedAt
) {
    public static OnboardingCompletionStatusResponse from(OnboardingCompletionStatus status) {
        return new OnboardingCompletionStatusResponse(status.status(), status.updatedAt());
    }
}
