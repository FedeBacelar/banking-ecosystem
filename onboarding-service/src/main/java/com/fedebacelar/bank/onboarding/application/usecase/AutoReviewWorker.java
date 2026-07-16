package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoReviewWorker {
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final AutoReviewService autoReviewService;
    private final OnboardingReviewPolicyPort reviewPolicy;
    private final Clock clock;
    private final OnboardingTelemetryPort telemetry;

    public AutoReviewWorker(OnboardingWorkItemRepositoryPort workItemRepository, AutoReviewService autoReviewService,
                            OnboardingReviewPolicyPort reviewPolicy, OnboardingTelemetryPort telemetry, Clock clock) {
        this.workItemRepository = workItemRepository;
        this.autoReviewService = autoReviewService;
        this.reviewPolicy = reviewPolicy;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${onboarding.review.worker-delay:PT1S}")
    public void processDueReviews() {
        for (int processed = 0; processed < reviewPolicy.workerBatchSize(); processed++) {
            OnboardingWorkItem item = workItemRepository
                    .claimNext(WorkflowJobType.AUTO_REVIEW, Instant.now(clock), reviewPolicy.workerLease())
                    .orElse(null);
            if (item == null) {
                return;
            }
            telemetry.observeWorkerExecution(OnboardingTelemetryPort.WorkType.AUTO_REVIEW, () -> {
                telemetry.recordWorkClaimed(OnboardingTelemetryPort.WorkType.AUTO_REVIEW);
                try {
                    autoReviewService.execute(item);
                } catch (RuntimeException exception) {
                    autoReviewService.handleTechnicalFailure(item, "AUTO_REVIEW_EXECUTION_ERROR");
                }
            });
        }
    }
}
