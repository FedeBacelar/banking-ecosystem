package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;

public interface CredentialInvitationBacklogProjection {
    long getPendingCount();

    Instant getOldestCreatedAt();
}
