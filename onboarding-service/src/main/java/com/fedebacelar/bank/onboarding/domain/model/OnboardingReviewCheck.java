package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckOutcome;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckType;
import java.time.Instant;
import java.util.UUID;

public record OnboardingReviewCheck(
        UUID id,
        UUID applicationId,
        ReviewCheckType checkType,
        ReviewCheckExecutionMode executionMode,
        ReviewCheckExecutionStatus executionStatus,
        ReviewCheckOutcome outcome,
        boolean blocking,
        String policyVersion,
        String provider,
        String reasonCode,
        int attempts,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static OnboardingReviewCheck completed(
            UUID applicationId,
            ReviewCheckType checkType,
            ReviewCheckExecutionMode executionMode,
            ReviewCheckOutcome outcome,
            boolean blocking,
            String policyVersion,
            String provider,
            String reasonCode,
            Instant now
    ) {
        return new OnboardingReviewCheck(
                UUID.randomUUID(), applicationId, checkType, executionMode,
                ReviewCheckExecutionStatus.COMPLETED, outcome, blocking, policyVersion,
                provider, reasonCode, 1, now, now, now, now, 0L
        );
    }
}
