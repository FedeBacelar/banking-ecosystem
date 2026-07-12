package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.PayloadCipherPort;
import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MagicLinkDeliveryWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagicLinkDeliveryWorker.class);

    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final MagicLinkDeliveryRepositoryPort deliveryRepository;
    private final PayloadCipherPort payloadCipher;
    private final NotificationPort notificationPort;
    private final MagicLinkDeliveryCompletionService completionService;
    private final MagicLinkDeliveryPolicyPort deliveryPolicy;
    private final Clock clock;

    public MagicLinkDeliveryWorker(
            OnboardingWorkItemRepositoryPort workItemRepository,
            MagicLinkDeliveryRepositoryPort deliveryRepository,
            PayloadCipherPort payloadCipher,
            NotificationPort notificationPort,
            MagicLinkDeliveryCompletionService completionService,
            MagicLinkDeliveryPolicyPort deliveryPolicy,
            Clock clock
    ) {
        this.workItemRepository = workItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.payloadCipher = payloadCipher;
        this.notificationPort = notificationPort;
        this.completionService = completionService;
        this.deliveryPolicy = deliveryPolicy;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${onboarding.notification.worker-delay:PT1S}")
    public void processPendingDeliveries() {
        for (int processed = 0; processed < deliveryPolicy.workerBatchSize(); processed++) {
            OnboardingWorkItem item = workItemRepository.claimNext(
                    WorkflowJobType.MAGIC_LINK_DELIVERY,
                    Instant.now(clock),
                    deliveryPolicy.workerLease()
            ).orElse(null);
            if (item == null) {
                return;
            }
            deliver(item);
        }
    }

    private void deliver(OnboardingWorkItem item) {
        Instant now = Instant.now(clock);
        try {
            MagicLinkDelivery delivery = deliveryRepository.findByApplicationId(item.applicationId())
                    .orElseThrow(() -> new IllegalStateException("Magic-link delivery payload is missing."));
            if (delivery.sentAt() != null) {
                completionService.succeed(item, delivery, now);
                return;
            }
            if (delivery.expired(now) || delivery.encryptedMagicLink() == null) {
                completionService.fail(item, "MAGIC_LINK_DELIVERY_EXPIRED", now);
                return;
            }

            notificationPort.sendMagicLink(
                    delivery.deliveryId(),
                    delivery.applicationId(),
                    delivery.recipient(),
                    payloadCipher.decrypt(delivery.encryptedMagicLink()),
                    Duration.between(now, delivery.expiresAt())
            );
            completionService.succeed(item, delivery, Instant.now(clock));
        } catch (RuntimeException exception) {
            handleFailure(item, exception, now);
        }
    }

    private void handleFailure(OnboardingWorkItem item, RuntimeException exception, Instant now) {
        LOGGER.warn(
                "Magic-link delivery failed for applicationId={} errorType={}",
                item.applicationId(), exception.getClass().getSimpleName()
        );
        if (item.attempts() >= deliveryPolicy.maxAttempts()) {
            completionService.fail(item, "MAGIC_LINK_DELIVERY_FAILED", now);
            return;
        }
        completionService.retry(
                item,
                "MAGIC_LINK_DELIVERY_RETRY",
                now.plus(deliveryPolicy.retryDelay(item.attempts())),
                now
        );
    }
}
