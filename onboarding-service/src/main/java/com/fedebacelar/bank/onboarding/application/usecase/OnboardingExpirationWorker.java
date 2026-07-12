package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OnboardingExpirationWorker {
    private static final Set<OnboardingApplicationStatus> EXPIRABLE = EnumSet.of(
            OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING,
            OnboardingApplicationStatus.IN_PROGRESS,
            OnboardingApplicationStatus.SUBMITTED,
            OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW,
            OnboardingApplicationStatus.REVIEW_FAILED
    );

    private final OnboardingApplicationRepositoryPort applications;
    private final OnboardingStatusHistoryRepositoryPort history;
    private final OnboardingUniquenessReservationPort reservations;
    private final OnboardingWorkItemRepositoryPort workItems;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final int batchSize;

    public OnboardingExpirationWorker(
            OnboardingApplicationRepositoryPort applications,
            OnboardingStatusHistoryRepositoryPort history,
            OnboardingUniquenessReservationPort reservations,
            OnboardingWorkItemRepositoryPort workItems,
            TransactionTemplate transactions,
            Clock clock,
            @Value("${onboarding.expiration.batch-size:100}") int batchSize
    ) {
        this.applications = applications;
        this.history = history;
        this.reservations = reservations;
        this.workItems = workItems;
        this.transactions = transactions;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${onboarding.expiration.worker-delay:PT1M}")
    public void expireApplications() {
        Instant now = Instant.now(clock);
        applications.findExpiredActiveApplications(now, EXPIRABLE, batchSize)
                .forEach(application -> transactions.executeWithoutResult(status -> expire(application.id(), now)));
    }

    private void expire(java.util.UUID applicationId, Instant now) {
        OnboardingApplication current = applications.findById(applicationId).orElse(null);
        if (current == null || !EXPIRABLE.contains(current.status()) || current.expiresAt().isAfter(now)) {
            return;
        }
        OnboardingApplication expired = applications.save(current.expire(now));
        history.save(OnboardingStatusHistory.transition(
                current.id(), current.status(), expired.status(), "APPLICATION_EXPIRED",
                OnboardingActorType.SYSTEM, now
        ));
        reservations.releaseByApplicationId(current.id(), now);
        for (WorkflowJobType type : WorkflowJobType.values()) {
            workItems.findByApplicationIdAndJobType(current.id(), type)
                    .filter(item -> item.status() != WorkflowJobStatus.SUCCEEDED
                            && item.status() != WorkflowJobStatus.FAILED)
                    .ifPresent(item -> workItems.save(item.fail("APPLICATION_EXPIRED", now)));
        }
    }
}
