package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingWorkItemRepositoryPort {
    OnboardingWorkItem save(OnboardingWorkItem workItem);
    Optional<OnboardingWorkItem> findByApplicationIdAndJobType(UUID applicationId, WorkflowJobType jobType);
    Optional<OnboardingWorkItem> claimNext(WorkflowJobType jobType, Instant now, Duration lease);
}
