package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import java.time.Instant;
import java.util.UUID;

public record OnboardingSubmissionResponse(UUID applicationId, String status, Instant submittedAt, Instant updatedAt) {
    public OnboardingSubmission toDomain() {
        return new OnboardingSubmission(applicationId, status, submittedAt, updatedAt);
    }
}
