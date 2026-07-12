package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record OnboardingSubmissionResponse(
        UUID applicationId,
        OnboardingApplicationStatus status,
        Instant submittedAt,
        Instant updatedAt
) {
    public static OnboardingSubmissionResponse from(OnboardingSubmissionDetails details) {
        return new OnboardingSubmissionResponse(details.applicationId(), details.status(), details.submittedAt(), details.updatedAt());
    }
}
