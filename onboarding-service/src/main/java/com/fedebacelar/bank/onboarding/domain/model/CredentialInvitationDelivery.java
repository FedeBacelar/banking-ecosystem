package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record CredentialInvitationDelivery(
        UUID id,
        UUID applicationId,
        String idempotencyKeyHash,
        WorkflowJobStatus status,
        int attempts,
        Instant nextAttemptAt,
        Instant lockedUntil,
        String lastErrorCode,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static CredentialInvitationDelivery pending(
            UUID applicationId,
            String idempotencyKeyHash,
            Instant now
    ) {
        return new CredentialInvitationDelivery(
                UUID.randomUUID(), applicationId, idempotencyKeyHash, WorkflowJobStatus.PENDING,
                0, now, null, null, null, now, now, 0L
        );
    }

    public CredentialInvitationDelivery claim(Instant now, Duration lease) {
        return new CredentialInvitationDelivery(
                id, applicationId, idempotencyKeyHash, WorkflowJobStatus.RUNNING,
                attempts + 1, nextAttemptAt, now.plus(lease), null, sentAt,
                createdAt, now, version
        );
    }

    public CredentialInvitationDelivery succeed(Instant now) {
        return new CredentialInvitationDelivery(
                id, applicationId, idempotencyKeyHash, WorkflowJobStatus.SUCCEEDED,
                attempts, nextAttemptAt, null, null, now, createdAt, now, version
        );
    }

    public CredentialInvitationDelivery retry(String errorCode, Instant retryAt, Instant now) {
        return new CredentialInvitationDelivery(
                id, applicationId, idempotencyKeyHash, WorkflowJobStatus.RETRY_WAIT,
                attempts, retryAt, null, errorCode, sentAt, createdAt, now, version
        );
    }

    public CredentialInvitationDelivery fail(String errorCode, Instant now) {
        return new CredentialInvitationDelivery(
                id, applicationId, idempotencyKeyHash, WorkflowJobStatus.FAILED,
                attempts, nextAttemptAt, null, errorCode, sentAt, createdAt, now, version
        );
    }
}
