package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MagicLinkDeliveryCompletionService {
    private final MagicLinkDeliveryRepositoryPort deliveryRepository;
    private final OnboardingWorkItemRepositoryPort workItemRepository;

    public MagicLinkDeliveryCompletionService(
            MagicLinkDeliveryRepositoryPort deliveryRepository,
            OnboardingWorkItemRepositoryPort workItemRepository
    ) {
        this.deliveryRepository = deliveryRepository;
        this.workItemRepository = workItemRepository;
    }

    @Transactional
    public void succeed(OnboardingWorkItem item, MagicLinkDelivery delivery, Instant now) {
        deliveryRepository.save(delivery.markSent(now));
        workItemRepository.save(item.succeed(now));
    }

    @Transactional
    public void retry(OnboardingWorkItem item, String errorCode, Instant nextAttemptAt, Instant now) {
        workItemRepository.save(item.retry(errorCode, nextAttemptAt, now));
    }

    @Transactional
    public void fail(OnboardingWorkItem item, String errorCode, Instant now) {
        deliveryRepository.findByApplicationId(item.applicationId())
                .filter(delivery -> delivery.encryptedMagicLink() != null)
                .map(delivery -> delivery.discardPayload(now))
                .ifPresent(deliveryRepository::save);
        workItemRepository.save(item.fail(errorCode, now));
    }
}
