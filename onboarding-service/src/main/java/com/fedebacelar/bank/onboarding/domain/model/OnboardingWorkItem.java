package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record OnboardingWorkItem(
        UUID id,
        UUID applicationId,
        WorkflowJobType jobType,
        WorkflowJobStatus status,
        int attempts,
        Instant nextAttemptAt,
        Instant lockedUntil,
        String lastErrorCode,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static OnboardingWorkItem pending(UUID applicationId, WorkflowJobType jobType, Instant now) {
        return new OnboardingWorkItem(
                UUID.randomUUID(), applicationId, jobType, WorkflowJobStatus.PENDING,
                0, now, null, null, now, now, 0L
        );
    }

    public OnboardingWorkItem claim(Instant now, Duration lease) {
        return new OnboardingWorkItem(id, applicationId, jobType, WorkflowJobStatus.RUNNING,
                attempts + 1, nextAttemptAt, now.plus(lease), null, createdAt, now, version);
    }

    public OnboardingWorkItem succeed(Instant now) {
        return new OnboardingWorkItem(id, applicationId, jobType, WorkflowJobStatus.SUCCEEDED,
                attempts, nextAttemptAt, null, null, createdAt, now, version);
    }

    public OnboardingWorkItem retry(String errorCode, Instant nextAttemptAt, Instant now) {
        return new OnboardingWorkItem(id, applicationId, jobType, WorkflowJobStatus.RETRY_WAIT,
                attempts, nextAttemptAt, null, errorCode, createdAt, now, version);
    }

    public OnboardingWorkItem fail(String errorCode, Instant now) {
        return new OnboardingWorkItem(id, applicationId, jobType, WorkflowJobStatus.FAILED,
                attempts, nextAttemptAt, null, errorCode, createdAt, now, version);
    }

    public OnboardingWorkItem reset(Instant now) {
        return new OnboardingWorkItem(id, applicationId, jobType, WorkflowJobStatus.PENDING,
                0, now, null, null, createdAt, now, version);
    }
}
