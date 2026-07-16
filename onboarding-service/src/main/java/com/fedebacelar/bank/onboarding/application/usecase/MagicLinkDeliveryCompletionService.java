package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MagicLinkDeliveryCompletionService {
    private final MagicLinkDeliveryRepositoryPort deliveryRepository;
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final OnboardingTelemetryPort telemetry;

    public MagicLinkDeliveryCompletionService(
            MagicLinkDeliveryRepositoryPort deliveryRepository,
            OnboardingWorkItemRepositoryPort workItemRepository,
            OnboardingTelemetryPort telemetry
    ) {
        this.deliveryRepository = deliveryRepository;
        this.workItemRepository = workItemRepository;
        this.telemetry = telemetry;
    }

    @Transactional
    public void succeed(OnboardingWorkItem item, MagicLinkDelivery delivery, Instant now) {
        deliveryRepository.save(delivery.markSent(now));
        workItemRepository.save(item.succeed(now));
        telemetry.recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.MAGIC_LINK_DELIVERY,
                OnboardingTelemetryPort.WorkOutcome.SUCCEEDED
        );
    }

    @Transactional
    public void retry(OnboardingWorkItem item, String errorCode, Instant nextAttemptAt, Instant now) {
        workItemRepository.save(item.retry(errorCode, nextAttemptAt, now));
        telemetry.recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.MAGIC_LINK_DELIVERY,
                OnboardingTelemetryPort.WorkOutcome.RETRY
        );
    }

    @Transactional
    public void fail(OnboardingWorkItem item, String errorCode, Instant now) {
        deliveryRepository.findByApplicationId(item.applicationId())
                .filter(delivery -> delivery.encryptedMagicLink() != null)
                .map(delivery -> delivery.discardPayload(now))
                .ifPresent(deliveryRepository::save);
        workItemRepository.save(item.fail(errorCode, now));
        telemetry.recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.MAGIC_LINK_DELIVERY,
                OnboardingTelemetryPort.WorkOutcome.EXHAUSTED
        );
    }
}
