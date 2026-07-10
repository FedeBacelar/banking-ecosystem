package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import java.time.Instant;
import java.util.UUID;

public record OnboardingProvisioningStep(
        UUID id,
        UUID applicationId,
        ProvisioningStepType stepType,
        ProvisioningStepStatus status,
        String requestHash,
        String externalReference,
        int attempts,
        Instant nextAttemptAt,
        String lastErrorCode,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static OnboardingProvisioningStep pending(UUID applicationId, ProvisioningStepType type, Instant now) {
        return new OnboardingProvisioningStep(UUID.randomUUID(), applicationId, type, ProvisioningStepStatus.PENDING,
                null, null, 0, now, null, null, null, now, now, 0L);
    }

    public OnboardingProvisioningStep start(String newRequestHash, Instant now) {
        return new OnboardingProvisioningStep(id, applicationId, stepType, ProvisioningStepStatus.RUNNING,
                newRequestHash, externalReference, attempts + 1, nextAttemptAt, null,
                startedAt == null ? now : startedAt, null, createdAt, now, version);
    }

    public OnboardingProvisioningStep succeed(String reference, Instant now) {
        return new OnboardingProvisioningStep(id, applicationId, stepType, ProvisioningStepStatus.SUCCEEDED,
                requestHash, reference, attempts, nextAttemptAt, null, startedAt, now, createdAt, now, version);
    }

    public OnboardingProvisioningStep retry(String errorCode, Instant retryAt, Instant now) {
        return new OnboardingProvisioningStep(id, applicationId, stepType, ProvisioningStepStatus.RETRY_WAIT,
                requestHash, externalReference, attempts, retryAt, errorCode, startedAt, null, createdAt, now, version);
    }

    public OnboardingProvisioningStep fail(String errorCode, Instant now) {
        return new OnboardingProvisioningStep(id, applicationId, stepType, ProvisioningStepStatus.FAILED,
                requestHash, externalReference, attempts, nextAttemptAt, errorCode, startedAt, null, createdAt, now, version);
    }

    public OnboardingProvisioningStep reset(Instant now) {
        return new OnboardingProvisioningStep(id, applicationId, stepType, ProvisioningStepStatus.PENDING,
                requestHash, externalReference, attempts, now, null, startedAt, completedAt, createdAt, now, version);
    }

    public OnboardingProvisioningStep restoreAfterInvitationFailure(String errorCode, Instant now) {
        return new OnboardingProvisioningStep(id, applicationId, stepType, ProvisioningStepStatus.SUCCEEDED,
                requestHash, externalReference, attempts, nextAttemptAt, errorCode, startedAt, completedAt,
                createdAt, now, version);
    }
}
