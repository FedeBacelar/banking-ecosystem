package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.PayloadCipherPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MagicLinkDeliveryWorkerTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final Duration LEASE = Duration.ofMinutes(1);

    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final MagicLinkDeliveryRepositoryPort deliveries = mock(MagicLinkDeliveryRepositoryPort.class);
    private final PayloadCipherPort cipher = mock(PayloadCipherPort.class);
    private final NotificationPort notifications = mock(NotificationPort.class);
    private final MagicLinkDeliveryPolicyPort policy = mock(MagicLinkDeliveryPolicyPort.class);
    private final MagicLinkDeliveryCompletionService completion =
            new MagicLinkDeliveryCompletionService(deliveries, workItems);
    private final MagicLinkDeliveryWorker worker = new MagicLinkDeliveryWorker(
            workItems, deliveries, cipher, notifications, completion, policy,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    private OnboardingWorkItem item;
    private MagicLinkDelivery delivery;

    @BeforeEach
    void setUp() {
        UUID applicationId = UUID.randomUUID();
        item = OnboardingWorkItem.pending(applicationId, WorkflowJobType.MAGIC_LINK_DELIVERY, NOW.minusSeconds(5))
                .claim(NOW.minusSeconds(1), LEASE);
        delivery = MagicLinkDelivery.pending(
                applicationId, "person@example.com", "encrypted-payload", NOW.plusSeconds(1800), NOW.minusSeconds(10)
        );
        when(policy.workerBatchSize()).thenReturn(1);
        when(policy.workerLease()).thenReturn(LEASE);
        when(policy.maxAttempts()).thenReturn(3);
        when(policy.retryDelay(anyInt())).thenReturn(Duration.ofSeconds(5));
        when(workItems.claimNext(WorkflowJobType.MAGIC_LINK_DELIVERY, NOW, LEASE))
                .thenReturn(Optional.of(item));
        when(deliveries.findByApplicationId(applicationId)).thenReturn(Optional.of(delivery));
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workItems.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cipher.decrypt("encrypted-payload")).thenReturn("https://bank.example/onboarding/continue#token=secret");
    }

    @Test
    void decryptsOnlyForDeliveryAndClearsThePersistedPayloadAfterSuccess() {
        AtomicReference<MagicLinkDelivery> saved = new AtomicReference<>();
        when(deliveries.save(any())).thenAnswer(invocation -> {
            MagicLinkDelivery value = invocation.getArgument(0);
            saved.set(value);
            return value;
        });

        worker.processPendingDeliveries();

        verify(notifications).sendMagicLink(
                delivery.deliveryId(), delivery.applicationId(), "person@example.com",
                "https://bank.example/onboarding/continue#token=secret", Duration.ofMinutes(30)
        );
        assertThat(saved.get().encryptedMagicLink()).isNull();
        assertThat(saved.get().sentAt()).isEqualTo(NOW);
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                savedItem -> savedItem.status() == WorkflowJobStatus.SUCCEEDED
        ));
    }

    @Test
    void schedulesAConfiguredRetryWithoutDiscardingTheEncryptedPayload() {
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp unavailable"))
                .when(notifications).sendMagicLink(any(), any(), any(), any(), any());

        worker.processPendingDeliveries();

        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(savedItem ->
                savedItem.status() == WorkflowJobStatus.RETRY_WAIT
                        && savedItem.nextAttemptAt().equals(NOW.plusSeconds(5))
        ));
        verify(deliveries, never()).save(any());
    }

    @Test
    void discardsAnExpiredPayloadAndFailsTheWorkItem() {
        delivery = MagicLinkDelivery.pending(
                item.applicationId(), "person@example.com", "expired-payload", NOW, NOW.minusSeconds(10)
        );
        when(deliveries.findByApplicationId(item.applicationId())).thenReturn(Optional.of(delivery));
        AtomicReference<MagicLinkDelivery> saved = new AtomicReference<>();
        when(deliveries.save(any())).thenAnswer(invocation -> {
            MagicLinkDelivery value = invocation.getArgument(0);
            saved.set(value);
            return value;
        });

        worker.processPendingDeliveries();

        verify(notifications, never()).sendMagicLink(any(), any(), any(), any(), any());
        assertThat(saved.get().encryptedMagicLink()).isNull();
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                savedItem -> savedItem.status() == WorkflowJobStatus.FAILED
        ));
    }
}
