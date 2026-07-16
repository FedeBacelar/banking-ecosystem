package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.PayloadCipherPort;
import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MagicLinkDeliveryWorker {
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final MagicLinkDeliveryRepositoryPort deliveryRepository;
    private final PayloadCipherPort payloadCipher;
    private final NotificationPort notificationPort;
    private final MagicLinkDeliveryCompletionService completionService;
    private final MagicLinkDeliveryPolicyPort deliveryPolicy;
    private final Clock clock;
    private final OnboardingTelemetryPort telemetry;

    public MagicLinkDeliveryWorker(
            OnboardingWorkItemRepositoryPort workItemRepository,
            MagicLinkDeliveryRepositoryPort deliveryRepository,
            PayloadCipherPort payloadCipher,
            NotificationPort notificationPort,
            MagicLinkDeliveryCompletionService completionService,
            MagicLinkDeliveryPolicyPort deliveryPolicy,
            OnboardingTelemetryPort telemetry,
            Clock clock
    ) {
        this.workItemRepository = workItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.payloadCipher = payloadCipher;
        this.notificationPort = notificationPort;
        this.completionService = completionService;
        this.deliveryPolicy = deliveryPolicy;
        this.telemetry = telemetry;
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
            telemetry.observeWorkerExecution(OnboardingTelemetryPort.WorkType.MAGIC_LINK_DELIVERY, () -> {
                telemetry.recordWorkClaimed(OnboardingTelemetryPort.WorkType.MAGIC_LINK_DELIVERY);
                deliver(item);
            });
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
