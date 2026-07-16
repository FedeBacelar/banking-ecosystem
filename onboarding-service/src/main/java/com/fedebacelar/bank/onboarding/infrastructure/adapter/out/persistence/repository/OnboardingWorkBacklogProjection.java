package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import java.time.Instant;

public interface OnboardingWorkBacklogProjection {
    WorkflowJobType getJobType();

    long getPendingCount();

    Instant getOldestCreatedAt();
}
