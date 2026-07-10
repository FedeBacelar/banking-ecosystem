package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import java.time.Instant;
import java.util.UUID;

public record OnboardingSubmissionResponse(UUID applicationId, String status, Instant submittedAt, Instant updatedAt) {
    public static OnboardingSubmissionResponse from(OnboardingSubmission submission) {
        return new OnboardingSubmissionResponse(submission.applicationId(), submission.status(), submission.submittedAt(), submission.updatedAt());
    }
}
