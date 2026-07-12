package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoReviewWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoReviewWorker.class);
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final AutoReviewService autoReviewService;
    private final OnboardingReviewPolicyPort reviewPolicy;
    private final Clock clock;

    public AutoReviewWorker(OnboardingWorkItemRepositoryPort workItemRepository, AutoReviewService autoReviewService,
                            OnboardingReviewPolicyPort reviewPolicy, Clock clock) {
        this.workItemRepository = workItemRepository;
        this.autoReviewService = autoReviewService;
        this.reviewPolicy = reviewPolicy;
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
            try {
                autoReviewService.execute(item);
            } catch (RuntimeException exception) {
                LOGGER.warn("AUTO review failed for applicationId={} errorType={}", item.applicationId(), exception.getClass().getSimpleName());
                autoReviewService.handleTechnicalFailure(item, "AUTO_REVIEW_EXECUTION_ERROR");
            }
        }
    }
}
