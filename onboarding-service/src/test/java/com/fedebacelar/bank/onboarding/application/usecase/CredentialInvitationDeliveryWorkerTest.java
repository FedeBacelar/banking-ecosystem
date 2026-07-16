package com.fedebacelar.bank.onboarding.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CredentialInvitationDeliveryWorkerTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final Duration LEASE = Duration.ofMinutes(1);

    private final CredentialInvitationDeliveryRepositoryPort deliveries = mock(
            CredentialInvitationDeliveryRepositoryPort.class
    );
    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingProvisioningStepRepositoryPort steps = mock(OnboardingProvisioningStepRepositoryPort.class);
    private final CredentialProvisioningPort credentials = mock(CredentialProvisioningPort.class);
    private final CredentialInvitationDeliveryPolicyPort policy = mock(CredentialInvitationDeliveryPolicyPort.class);
    private final OnboardingTelemetryPort telemetry = mock(OnboardingTelemetryPort.class);
    private final CredentialInvitationDeliveryWorker worker = new CredentialInvitationDeliveryWorker(
            deliveries, applications, steps, credentials, policy, telemetry, Clock.fixed(NOW, ZoneOffset.UTC)
    );

    private OnboardingApplication application;
    private CredentialInvitationDelivery delivery;
    private OnboardingProvisioningStep invitation;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(telemetry).observeWorkerExecution(any(), any());
        application = credentialPendingApplication();
        delivery = CredentialInvitationDelivery.pending(
                application.id(), "idempotency-hash", NOW.minusSeconds(10)
        ).claim(NOW.minusSeconds(1), LEASE);
        invitation = OnboardingProvisioningStep.pending(
                application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL, NOW.minusSeconds(180)
        ).start("hash", NOW.minusSeconds(170)).succeed("keycloak-user-id", NOW.minusSeconds(120));

        when(policy.workerBatchSize()).thenReturn(1);
        when(policy.workerLease()).thenReturn(LEASE);
        when(policy.maxAttempts()).thenReturn(3);
        when(policy.retryDelay(anyInt())).thenReturn(Duration.ofSeconds(5));
        when(deliveries.claimNext(NOW, LEASE)).thenReturn(Optional.of(delivery));
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(applications.findById(application.id())).thenReturn(Optional.of(application));
        when(steps.findByApplicationIdAndStepType(
                application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL
        )).thenReturn(Optional.of(invitation));
    }

    @Test
    void sendsTheInvitationAndCompletesTheDurableIntent() {
        worker.processPendingDeliveries();

        verify(credentials).sendCredentialSetupEmail("keycloak-user-id");
        verify(deliveries).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == WorkflowJobStatus.SUCCEEDED && NOW.equals(saved.sentAt())
        ));
    }

    @Test
    void retriesWithASanitizedErrorCodeAfterATransientFailure() {
        doThrow(new IllegalStateException("provider included sensitive detail"))
                .when(credentials).sendCredentialSetupEmail("keycloak-user-id");

        worker.processPendingDeliveries();

        verify(deliveries).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == WorkflowJobStatus.RETRY_WAIT
                        && "CREDENTIAL_INVITATION_DELIVERY_RETRY".equals(saved.lastErrorCode())
                        && NOW.plusSeconds(5).equals(saved.nextAttemptAt())
        ));
    }

    @Test
    void marksTheIntentFailedAfterTheConfiguredAttempts() {
        delivery = delivery.claim(NOW.minusMillis(500), LEASE).claim(NOW.minusMillis(250), LEASE);
        when(deliveries.claimNext(NOW, LEASE)).thenReturn(Optional.of(delivery));
        doThrow(new IllegalStateException("provider included sensitive detail"))
                .when(credentials).sendCredentialSetupEmail("keycloak-user-id");

        worker.processPendingDeliveries();

        verify(deliveries).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == WorkflowJobStatus.FAILED
                        && "CREDENTIAL_INVITATION_DELIVERY_FAILED".equals(saved.lastErrorCode())
        ));
    }

    @Test
    void doesNotSendWhenCredentialSetupIsNoLongerPending() {
        when(applications.findById(application.id())).thenReturn(Optional.of(application.complete(NOW)));

        worker.processPendingDeliveries();

        verify(credentials, never()).sendCredentialSetupEmail(any());
        verify(deliveries).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == WorkflowJobStatus.FAILED
                        && "CREDENTIAL_INVITATION_NOT_APPLICABLE".equals(saved.lastErrorCode())
        ));
    }

    private OnboardingApplication credentialPendingApplication() {
        return OnboardingApplication.start(
                "person@example.com", "magic", NOW.plusSeconds(1800), NOW.plus(Duration.ofDays(15)), NOW.minusSeconds(200)
        ).verifyEmail("continuation-hash", NOW.plusSeconds(7200), NOW.minusSeconds(190))
                .submit(NOW.minusSeconds(180))
                .startAutomatedReview(NOW.minusSeconds(170))
                .approve(NOW.minusSeconds(160))
                .startProvisioning(NOW.minusSeconds(150))
                .markCredentialSetupPending(NOW.minusSeconds(120));
    }
}
