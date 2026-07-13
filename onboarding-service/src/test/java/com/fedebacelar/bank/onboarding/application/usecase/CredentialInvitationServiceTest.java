package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialInvitationCooldownException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidIdempotencyKeyException;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CredentialInvitationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final String CONTINUATION_TOKEN = "continuation-token";
    private static final String IDEMPOTENCY_KEY = "request-12345678";

    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingProvisioningStepRepositoryPort steps = mock(OnboardingProvisioningStepRepositoryPort.class);
    private final CredentialInvitationDeliveryRepositoryPort deliveries = mock(
            CredentialInvitationDeliveryRepositoryPort.class
    );
    private final CredentialInvitationDeliveryPolicyPort policy = mock(CredentialInvitationDeliveryPolicyPort.class);
    private final TokenHashingPort hashing = mock(TokenHashingPort.class);
    private final OnboardingApplication application = credentialPendingApplication();
    private final OnboardingProvisioningStep invitation = OnboardingProvisioningStep.pending(
            application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL, NOW.minusSeconds(180)
    ).start("hash", NOW.minusSeconds(170)).succeed("keycloak-user-id", NOW.minusSeconds(120));
    private final CredentialInvitationService service = new CredentialInvitationService(
            applications, steps, deliveries, policy, hashing, Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @BeforeEach
    void setUp() {
        when(hashing.hash(CONTINUATION_TOKEN)).thenReturn("continuation-hash");
        when(hashing.hash(IDEMPOTENCY_KEY)).thenReturn("idempotency-hash");
        when(applications.findByContinuationTokenHashForUpdate("continuation-hash"))
                .thenReturn(Optional.of(application));
        when(steps.findByApplicationIdAndStepType(
                application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL
        )).thenReturn(Optional.of(invitation));
        when(deliveries.findByApplicationIdAndIdempotencyKeyHash(
                application.id(), "idempotency-hash"
        )).thenReturn(Optional.empty());
        when(deliveries.findLatestByApplicationId(application.id())).thenReturn(Optional.empty());
        when(deliveries.findActiveByApplicationIdForUpdate(application.id())).thenReturn(List.of());
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(policy.credentialInvitationCooldown()).thenReturn(Duration.ofMinutes(1));
    }

    @Test
    void persistsADeliveryIntentWithoutSendingInsideTheRequestTransaction() {
        AtomicReference<CredentialInvitationDelivery> saved = new AtomicReference<>();
        when(deliveries.save(any())).thenAnswer(invocation -> {
            CredentialInvitationDelivery delivery = invocation.getArgument(0);
            saved.set(delivery);
            return delivery;
        });

        var result = service.resend(CONTINUATION_TOKEN, IDEMPOTENCY_KEY);

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING);
        assertThat(saved.get().applicationId()).isEqualTo(application.id());
        assertThat(saved.get().idempotencyKeyHash()).isEqualTo("idempotency-hash");
        assertThat(saved.get().status()).isEqualTo(WorkflowJobStatus.PENDING);
        assertThat(saved.get().attempts()).isZero();
    }

    @Test
    void returnsTheSameAcceptedOutcomeForAnExistingIdempotencyKeyEvenDuringCooldown() {
        CredentialInvitationDelivery existing = CredentialInvitationDelivery.pending(
                application.id(), "idempotency-hash", NOW.minusSeconds(5)
        );
        when(deliveries.findByApplicationIdAndIdempotencyKeyHash(
                application.id(), "idempotency-hash"
        )).thenReturn(Optional.of(existing));

        var result = service.resend(CONTINUATION_TOKEN, IDEMPOTENCY_KEY);

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING);
        verify(deliveries, never()).findLatestByApplicationId(any());
        verify(deliveries, never()).save(any());
    }

    @Test
    void replaysAnAcceptedKeyAfterTheApplicationHasCompleted() {
        OnboardingApplication completed = application.complete(NOW);
        CredentialInvitationDelivery existing = CredentialInvitationDelivery.pending(
                application.id(), "idempotency-hash", NOW.minusSeconds(5)
        );
        when(applications.findByContinuationTokenHashForUpdate("continuation-hash"))
                .thenReturn(Optional.of(completed));
        when(deliveries.findByApplicationIdAndIdempotencyKeyHash(
                application.id(), "idempotency-hash"
        )).thenReturn(Optional.of(existing));

        var result = service.resend(CONTINUATION_TOKEN, IDEMPOTENCY_KEY);

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.COMPLETED);
        verify(steps, never()).findByApplicationIdAndStepType(any(), any());
        verify(deliveries, never()).save(any());
    }

    @Test
    void rejectsANewRequestDuringCooldownAndReportsWholeRetryAfterSeconds() {
        CredentialInvitationDelivery previous = CredentialInvitationDelivery.pending(
                application.id(), "previous-key-hash", NOW.minusMillis(15_500)
        );
        when(deliveries.findLatestByApplicationId(application.id())).thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> service.resend(CONTINUATION_TOKEN, IDEMPOTENCY_KEY))
                .isInstanceOfSatisfying(CredentialInvitationCooldownException.class, exception ->
                        assertThat(exception.retryAfterSeconds()).isEqualTo(45L)
                );

        verify(deliveries, never()).save(any());
    }

    @Test
    void supersedesAnOlderRetryBeforeAcceptingANewIntent() {
        CredentialInvitationDelivery previous = CredentialInvitationDelivery.pending(
                application.id(), "previous-key-hash", NOW.minusSeconds(90)
        ).claim(NOW.minusSeconds(80), Duration.ofSeconds(20))
                .retry("CREDENTIAL_INVITATION_DELIVERY_RETRY", NOW.plusSeconds(30), NOW.minusSeconds(70));
        when(deliveries.findLatestByApplicationId(application.id())).thenReturn(Optional.of(previous));
        when(deliveries.findActiveByApplicationIdForUpdate(application.id())).thenReturn(List.of(previous));
        List<CredentialInvitationDelivery> saved = new ArrayList<>();
        when(deliveries.save(any())).thenAnswer(invocation -> {
            CredentialInvitationDelivery delivery = invocation.getArgument(0);
            saved.add(delivery);
            return delivery;
        });

        service.resend(CONTINUATION_TOKEN, IDEMPOTENCY_KEY);

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).status()).isEqualTo(WorkflowJobStatus.FAILED);
        assertThat(saved.get(0).lastErrorCode()).isEqualTo("CREDENTIAL_INVITATION_SUPERSEDED");
        assertThat(saved.get(1).status()).isEqualTo(WorkflowJobStatus.PENDING);
    }

    @Test
    void doesNotOverlapAWorkerThatStillOwnsTheDeliveryLease() {
        CredentialInvitationDelivery running = CredentialInvitationDelivery.pending(
                application.id(), "previous-key-hash", NOW.minusSeconds(90)
        ).claim(NOW.minusSeconds(10), Duration.ofSeconds(40));
        when(deliveries.findLatestByApplicationId(application.id())).thenReturn(Optional.of(running));
        when(deliveries.findActiveByApplicationIdForUpdate(application.id())).thenReturn(List.of(running));

        assertThatThrownBy(() -> service.resend(CONTINUATION_TOKEN, IDEMPOTENCY_KEY))
                .isInstanceOfSatisfying(CredentialInvitationCooldownException.class, exception ->
                        assertThat(exception.retryAfterSeconds()).isEqualTo(30L)
                );

        verify(deliveries, never()).save(any());
    }

    @Test
    void rejectsBlankIdempotencyKeysBeforeReadingTheApplication() {
        assertThatThrownBy(() -> service.resend(CONTINUATION_TOKEN, " "))
                .isInstanceOf(InvalidIdempotencyKeyException.class);

        verify(applications, never()).findByContinuationTokenHashForUpdate(any());
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
