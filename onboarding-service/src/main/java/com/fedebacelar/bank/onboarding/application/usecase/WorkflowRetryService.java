package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.mapper.OnboardingApplicationDetailsMapper;
import com.fedebacelar.bank.onboarding.application.port.in.RetryOnboardingWorkflowUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowRetryService implements RetryOnboardingWorkflowUseCase {
    private final OnboardingApplicationRepositoryPort applications;
    private final OnboardingStatusHistoryRepositoryPort history;
    private final OnboardingWorkItemRepositoryPort workItems;
    private final OnboardingProvisioningStepRepositoryPort steps;
    private final Clock clock;
    public WorkflowRetryService(OnboardingApplicationRepositoryPort applications, OnboardingStatusHistoryRepositoryPort history,
            OnboardingWorkItemRepositoryPort workItems, OnboardingProvisioningStepRepositoryPort steps, Clock clock) {
        this.applications = applications; this.history = history; this.workItems = workItems; this.steps = steps; this.clock = clock;
    }

    @Override @Transactional
    public OnboardingApplicationDetails retryReview(UUID applicationId) {
        Instant now = Instant.now(clock);
        OnboardingApplication current = require(applicationId);
        if (current.status() != OnboardingApplicationStatus.REVIEW_FAILED) {
            throw new InvalidOnboardingStatusTransitionException(current.status(), OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW);
        }
        OnboardingApplication next = applications.save(current.retryReview(now));
        saveHistory(current, next, "AUTO_REVIEW_RETRY_REQUESTED", now);
        workItems.findByApplicationIdAndJobType(applicationId, WorkflowJobType.AUTO_REVIEW)
                .ifPresent(item -> workItems.save(item.reset(now)));
        return OnboardingApplicationDetailsMapper.toDetails(next);
    }

    @Override @Transactional
    public OnboardingApplicationDetails retryProvisioning(UUID applicationId) {
        Instant now = Instant.now(clock);
        OnboardingApplication current = require(applicationId);
        if (current.status() != OnboardingApplicationStatus.PROVISIONING_FAILED) {
            throw new InvalidOnboardingStatusTransitionException(current.status(), OnboardingApplicationStatus.PROVISIONING);
        }
        OnboardingApplication next = applications.save(current.retryProvisioning(now));
        saveHistory(current, next, "PROVISIONING_RETRY_REQUESTED", now);
        steps.findByApplicationId(applicationId).stream()
                .filter(step -> step.status() == ProvisioningStepStatus.FAILED || step.status() == ProvisioningStepStatus.RETRY_WAIT)
                .findFirst().ifPresent(step -> steps.save(step.reset(now)));
        workItems.findByApplicationIdAndJobType(applicationId, WorkflowJobType.PROVISIONING)
                .ifPresent(item -> workItems.save(item.reset(now)));
        return OnboardingApplicationDetailsMapper.toDetails(next);
    }

    private OnboardingApplication require(UUID id) {
        return applications.findById(id).orElseThrow(() -> new OnboardingApplicationNotFoundException(id));
    }
    private void saveHistory(OnboardingApplication current, OnboardingApplication next, String reason, Instant now) {
        history.save(OnboardingStatusHistory.transition(current.id(), current.status(), next.status(), reason, OnboardingActorType.OPERATOR, now));
    }
}
