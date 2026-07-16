package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkFactoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingEmailRequestGuardPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.PayloadCipherPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenGeneratorPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingReviewMode;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OnboardingApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);

    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingStatusHistoryRepositoryPort history = mock(OnboardingStatusHistoryRepositoryPort.class);
    private final OnboardingEmailRequestGuardPort requestGuard = mock(OnboardingEmailRequestGuardPort.class);
    private final TokenGeneratorPort tokens = mock(TokenGeneratorPort.class);
    private final TokenHashingPort hashing = mock(TokenHashingPort.class);
    private final MagicLinkDeliveryRepositoryPort deliveries = mock(MagicLinkDeliveryRepositoryPort.class);
    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final PayloadCipherPort cipher = mock(PayloadCipherPort.class);
    private final MagicLinkFactoryPort magicLinks = mock(MagicLinkFactoryPort.class);
    private final OnboardingReviewPolicyPort reviewPolicy = mock(OnboardingReviewPolicyPort.class);
    private final OnboardingTelemetryPort telemetry = mock(OnboardingTelemetryPort.class);
    private final OnboardingApplicationService service = new OnboardingApplicationService(
            applications,
            history,
            requestGuard,
            tokens,
            hashing,
            deliveries,
            workItems,
            cipher,
            magicLinks,
            Clock.fixed(NOW, ZoneOffset.UTC),
            reviewPolicy,
            telemetry,
            30,
            120,
            15,
            REQUEST_COOLDOWN
    );

    @BeforeEach
    void setUp() {
        when(applications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workItems.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(history.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(hashing.hash(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));
        when(reviewPolicy.mode()).thenReturn(OnboardingReviewMode.AUTO);
        when(reviewPolicy.activePolicyVersion()).thenReturn("AR_DNI_SAVINGS_V1");
    }

    @Test
    void shouldPersistApplicationAndMagicLinkOutboxWithoutSendingEmailInline() {
        when(requestGuard.acquireAndRegister("person@example.com", NOW, REQUEST_COOLDOWN)).thenReturn(true);
        when(tokens.generate()).thenReturn("magic-token");
        when(magicLinks.create("magic-token"))
                .thenReturn("http://localhost:4200/onboarding/continue#token=magic-token");
        when(cipher.encrypt("http://localhost:4200/onboarding/continue#token=magic-token"))
                .thenReturn("encrypted-link");

        var result = service.start(new StartOnboardingApplicationCommand(" Person@Example.com "));

        assertThat(result.email()).isEqualTo("person@example.com");
        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING);
        ArgumentCaptor<MagicLinkDelivery> delivery = ArgumentCaptor.forClass(MagicLinkDelivery.class);
        verify(deliveries).save(delivery.capture());
        assertThat(delivery.getValue().encryptedMagicLink()).isEqualTo("encrypted-link");
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                item -> item.jobType() == WorkflowJobType.MAGIC_LINK_DELIVERY
        ));
        verify(telemetry).recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.CREATED);
    }

    @Test
    void shouldReturnExistingApplicationDuringRequestCooldownWithoutRotatingItsLink() {
        OnboardingApplication existing = pendingApplication();
        when(requestGuard.acquireAndRegister(existing.email(), NOW, REQUEST_COOLDOWN)).thenReturn(false);
        when(applications.findFirstByEmailAndStatusInOrderByCreatedAtDesc(anyString(), any()))
                .thenReturn(Optional.of(existing));

        var result = service.start(new StartOnboardingApplicationCommand(existing.email()));

        assertThat(result.id()).isEqualTo(existing.id());
        verifyNoInteractions(tokens, magicLinks, cipher, deliveries, workItems);
        verify(applications, never()).save(any());
    }

    @Test
    void shouldRotateAnExistingAccessLinkAndResetItsOutboxWorkItem() {
        OnboardingApplication existing = pendingApplication();
        MagicLinkDelivery previousDelivery = MagicLinkDelivery.pending(
                existing.id(), existing.email(), "old-encrypted-link", NOW.plusSeconds(300), NOW.minusSeconds(60)
        );
        OnboardingWorkItem previousWork = OnboardingWorkItem.pending(
                existing.id(), WorkflowJobType.MAGIC_LINK_DELIVERY, NOW.minusSeconds(60)
        ).claim(NOW.minusSeconds(50), Duration.ofMinutes(1)).fail("OLD_FAILURE", NOW.minusSeconds(40));
        when(requestGuard.acquireAndRegister(existing.email(), NOW, REQUEST_COOLDOWN)).thenReturn(true);
        when(applications.findFirstByEmailAndStatusInOrderByCreatedAtDesc(anyString(), any()))
                .thenReturn(Optional.of(existing));
        when(tokens.generate()).thenReturn("new-magic-token");
        when(magicLinks.create("new-magic-token"))
                .thenReturn("http://localhost:4200/onboarding/continue#token=new-magic-token");
        when(cipher.encrypt(anyString())).thenReturn("new-encrypted-link");
        when(deliveries.findByApplicationId(existing.id())).thenReturn(Optional.of(previousDelivery));
        when(workItems.findByApplicationIdAndJobType(existing.id(), WorkflowJobType.MAGIC_LINK_DELIVERY))
                .thenReturn(Optional.of(previousWork));

        service.start(new StartOnboardingApplicationCommand(existing.email()));

        verify(deliveries).save(org.mockito.ArgumentMatchers.argThat(delivery ->
                delivery.deliveryId() != previousDelivery.deliveryId()
                        && "new-encrypted-link".equals(delivery.encryptedMagicLink())
        ));
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(item ->
                item.id().equals(previousWork.id()) && item.attempts() == 0
        ));
    }

    @Test
    void shouldConsumeMagicLinkUnderDatabaseLockAndCreateContinuation() {
        OnboardingApplication application = pendingApplication();
        when(applications.findByMagicLinkTokenHashForUpdate("hash:magic-token"))
                .thenReturn(Optional.of(application));
        when(tokens.generate()).thenReturn("continuation-token");

        var result = service.consume(new ConsumeMagicLinkCommand("magic-token"));

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.IN_PROGRESS);
        assertThat(result.continuationToken()).isEqualTo("continuation-token");
        verify(applications).findByMagicLinkTokenHashForUpdate("hash:magic-token");
        verify(applications, never()).findByMagicLinkTokenHash("hash:magic-token");
    }

    @Test
    void shouldRejectSecondConsumptionOfTheSameMagicLink() {
        OnboardingApplication consumed = pendingApplication().verifyEmail(
                "old-continuation", NOW.plusSeconds(3600), NOW.minusSeconds(10)
        );
        when(applications.findByMagicLinkTokenHashForUpdate("hash:magic-token"))
                .thenReturn(Optional.of(consumed));

        assertThatThrownBy(() -> service.consume(new ConsumeMagicLinkCommand("magic-token")))
                .isInstanceOf(OnboardingMagicLinkAlreadyConsumedException.class);
    }

    @Test
    void shouldUseARefreshedMagicLinkToRecoverReadOnlyAccessAfterSubmission() {
        OnboardingApplication submitted = pendingApplication()
                .verifyEmail("old-continuation", NOW.plusSeconds(3600), NOW.minusSeconds(30))
                .submit(NOW.minusSeconds(20))
                .refreshAccessLink("hash:recovery-token", NOW.plusSeconds(1800), NOW.minusSeconds(10));
        when(applications.findByMagicLinkTokenHashForUpdate("hash:recovery-token"))
                .thenReturn(Optional.of(submitted));
        when(tokens.generate()).thenReturn("new-continuation");

        var result = service.consume(new ConsumeMagicLinkCommand("recovery-token"));

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.SUBMITTED);
        assertThat(result.continuationToken()).isEqualTo("new-continuation");
    }

    @Test
    void shouldPersistExpirationBeforeReportingExpiredMagicLink() {
        OnboardingApplication expiredLink = new OnboardingApplication(
                pendingApplication().id(),
                "person@example.com",
                OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING,
                "hash:magic-token",
                NOW.minusSeconds(1),
                null,
                null,
                null,
                null,
                NOW.plus(Duration.ofDays(1)),
                NOW.minus(Duration.ofDays(1)),
                NOW.minus(Duration.ofDays(1)),
                0L
        );
        when(applications.findByMagicLinkTokenHashForUpdate("hash:magic-token"))
                .thenReturn(Optional.of(expiredLink));

        assertThatThrownBy(() -> service.consume(new ConsumeMagicLinkCommand("magic-token")))
                .isInstanceOf(OnboardingMagicLinkExpiredException.class);

        verify(applications).save(org.mockito.ArgumentMatchers.argThat(
                application -> application.status() == OnboardingApplicationStatus.EXPIRED
        ));
    }

    @Test
    void shouldNotForceAnUnsupportedExpirationTransitionAfterProvisioningStarts() {
        OnboardingApplication provisioning = expiredProvisioningApplication();
        when(applications.findByContinuationTokenHash("hash:continuation-token"))
                .thenReturn(Optional.of(provisioning));

        assertThatThrownBy(() -> service.validate(new ValidateContinuationCommand("continuation-token")))
                .isInstanceOf(OnboardingContinuationExpiredException.class);

        verify(applications, never()).save(any());
    }

    @Test
    void shouldNotIssueAnUnusableAccessLinkForAnExpiredNonExpirableProcess() {
        OnboardingApplication provisioning = expiredProvisioningApplication();
        when(requestGuard.acquireAndRegister(provisioning.email(), NOW, REQUEST_COOLDOWN)).thenReturn(true);
        when(applications.findFirstByEmailAndStatusInOrderByCreatedAtDesc(anyString(), any()))
                .thenReturn(Optional.of(provisioning));

        var result = service.start(new StartOnboardingApplicationCommand(provisioning.email()));

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.PROVISIONING);
        verifyNoInteractions(tokens, magicLinks, cipher, deliveries, workItems);
        verify(applications, never()).save(any());
    }

    private OnboardingApplication expiredProvisioningApplication() {
        return OnboardingApplication.start(
                "person@example.com", "magic-hash", NOW.minus(Duration.ofDays(1)),
                NOW.minusSeconds(1), NOW.minus(Duration.ofDays(20))
        ).verifyEmail("hash:continuation-token", NOW.plusSeconds(3600), NOW.minus(Duration.ofDays(19)))
                .submit(NOW.minus(Duration.ofDays(18)))
                .startAutomatedReview(NOW.minus(Duration.ofDays(17)))
                .approve(NOW.minus(Duration.ofDays(16)))
                .startProvisioning(NOW.minus(Duration.ofDays(15)));
    }

    private OnboardingApplication pendingApplication() {
        return OnboardingApplication.start(
                "person@example.com",
                "hash:magic-token",
                NOW.plusSeconds(1800),
                NOW.plus(Duration.ofDays(15)),
                OnboardingReviewMode.AUTO,
                "AR_DNI_SAVINGS_V1",
                NOW.minusSeconds(60)
        );
    }
}
