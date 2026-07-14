package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.onboarding.application.view.OnboardingCompletionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;

public record OnboardingCompletionStatusResponse(
        OnboardingApplicationStatus status,
        Instant updatedAt
) {
    public static OnboardingCompletionStatusResponse from(OnboardingCompletionDetails details) {
        return new OnboardingCompletionStatusResponse(details.status(), details.updatedAt());
    }
}
