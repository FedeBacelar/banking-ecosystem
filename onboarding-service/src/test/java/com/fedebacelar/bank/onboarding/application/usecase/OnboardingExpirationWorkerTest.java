package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class OnboardingExpirationWorkerTest {
    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");

    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingStatusHistoryRepositoryPort history = mock(OnboardingStatusHistoryRepositoryPort.class);
    private final OnboardingUniquenessReservationPort reservations = mock(OnboardingUniquenessReservationPort.class);
    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final OnboardingTelemetryPort telemetry = mock(OnboardingTelemetryPort.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final OnboardingExpirationWorker worker = new OnboardingExpirationWorker(
            applications,
            history,
            reservations,
            workItems,
            telemetry,
            transactions,
            Clock.fixed(NOW, ZoneOffset.UTC),
            100
    );

    @BeforeEach
    void executeCallbacks() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(telemetry).observeExpirationExecution(any());
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());
    }

    @Test
    void doesNotCreateATraceWhenThereIsNothingToExpire() {
        when(applications.findExpiredActiveApplications(eq(NOW), anySet(), eq(100))).thenReturn(List.of());

        worker.expireApplications();

        verify(telemetry, never()).observeExpirationExecution(any());
        verify(transactions, never()).executeWithoutResult(any());
    }

    @Test
    void tracesAndRethrowsDiscoveryFailures() {
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(applications.findExpiredActiveApplications(eq(NOW), anySet(), eq(100))).thenThrow(failure);

        assertThatThrownBy(worker::expireApplications).isSameAs(failure);

        verify(telemetry).observeExpirationExecution(any());
        verify(transactions, never()).executeWithoutResult(any());
    }

    @Test
    void tracesOnlyTheBatchThatActuallyExpiresApplications() {
        OnboardingApplication application = OnboardingApplication.start(
                "expired@example.com",
                "magic-hash",
                NOW.minus(Duration.ofMinutes(1)),
                NOW.minus(Duration.ofSeconds(1)),
                NOW.minus(Duration.ofHours(1))
        );
        when(applications.findExpiredActiveApplications(eq(NOW), anySet(), anyInt()))
                .thenReturn(List.of(application));
        when(applications.findById(application.id())).thenReturn(Optional.of(application));
        when(applications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        worker.expireApplications();

        verify(telemetry).observeExpirationExecution(any());
        verify(reservations).releaseByApplicationId(application.id(), NOW);
        verify(history).save(any());
        verify(applications).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.status() == OnboardingApplicationStatus.EXPIRED
        ));
        assertThat(application.status()).isEqualTo(OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING);
    }
}
