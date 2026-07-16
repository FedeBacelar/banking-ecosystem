package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.model.CredentialSetupState;
import com.fedebacelar.bank.onboarding.application.port.out.AccountProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CustomerProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.IdentityProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingProvisioningProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class CredentialSetupReconcilerTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingProvisioningStepRepositoryPort steps = mock(OnboardingProvisioningStepRepositoryPort.class);
    private final OnboardingStatusHistoryRepositoryPort history = mock(OnboardingStatusHistoryRepositoryPort.class);
    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final CredentialProvisioningPort credentials = mock(CredentialProvisioningPort.class);
    private final CustomerProvisioningPort customers = mock(CustomerProvisioningPort.class);
    private final AccountProvisioningPort accounts = mock(AccountProvisioningPort.class);
    private final IdentityProvisioningPort identities = mock(IdentityProvisioningPort.class);
    private final OnboardingProvisioningProperties policy = new OnboardingProvisioningProperties();
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final OnboardingTelemetryPort telemetry = mock(OnboardingTelemetryPort.class);
    private final AtomicReference<OnboardingApplication> persistedApplication = new AtomicReference<>();
    private final Map<ProvisioningStepType, OnboardingProvisioningStep> persistedSteps =
            new EnumMap<>(ProvisioningStepType.class);

    private CredentialSetupReconciler reconciler;
    private OnboardingWorkItem item;
    private UUID customerId;
    private UUID accountId;
    private String userId;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());

        OnboardingApplication application = OnboardingApplication.start(
                "person@example.com", "magic", NOW.plusSeconds(1800), NOW.plus(Duration.ofDays(15)),
                NOW.minus(Duration.ofDays(2))
        ).verifyEmail("continuation", NOW.plusSeconds(7200), NOW.minus(Duration.ofDays(2)).plusSeconds(10))
                .submit(NOW.minus(Duration.ofDays(2)).plusSeconds(20))
                .startAutomatedReview(NOW.minus(Duration.ofDays(2)).plusSeconds(30))
                .approve(NOW.minus(Duration.ofDays(2)).plusSeconds(40))
                .startProvisioning(NOW.minus(Duration.ofDays(2)).plusSeconds(50))
                .markCredentialSetupPending(NOW.minus(Duration.ofDays(1)));
        persistedApplication.set(application);
        when(applications.findById(application.id())).thenAnswer(invocation -> Optional.of(persistedApplication.get()));
        when(applications.save(any())).thenAnswer(invocation -> {
            OnboardingApplication saved = invocation.getArgument(0);
            persistedApplication.set(saved);
            return saved;
        });
        when(steps.findByApplicationIdAndStepType(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(persistedSteps.get(invocation.getArgument(1))));
        when(steps.save(any())).thenAnswer(invocation -> {
            OnboardingProvisioningStep saved = invocation.getArgument(0);
            persistedSteps.put(saved.stepType(), saved);
            return saved;
        });
        when(workItems.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        userId = UUID.randomUUID().toString();
        succeeded(application.id(), ProvisioningStepType.CREATE_CUSTOMER, customerId.toString(), NOW.minus(Duration.ofDays(1)));
        succeeded(application.id(), ProvisioningStepType.OPEN_ACCOUNT, accountId.toString(), NOW.minus(Duration.ofDays(1)));
        succeeded(application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER, userId, NOW.minus(Duration.ofDays(1)));
        succeeded(application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL, userId, NOW.minus(Duration.ofDays(1)));
        item = OnboardingWorkItem.pending(application.id(), WorkflowJobType.CREDENTIAL_RECONCILIATION,
                NOW.minusSeconds(5)).claim(NOW.minusSeconds(1), Duration.ofMinutes(5));

        reconciler = new CredentialSetupReconciler(
                applications, steps, history, workItems, credentials, customers, accounts, identities,
                policy, transactions, telemetry, Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void waitsWithoutConsumingTechnicalRetriesWhileTheApplicantCompletesCredentials() {
        when(credentials.getCredentialSetupState(userId))
                .thenReturn(new CredentialSetupState(false, "pending-user"));

        reconciler.reconcile(item);

        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == WorkflowJobStatus.RETRY_WAIT
                        && saved.attempts() == 0
                        && saved.nextAttemptAt().equals(NOW.plusSeconds(30))
        ));
        verifyNoInteractions(customers, accounts, identities);
        assertThat(persistedApplication.get().status())
                .isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING);
        verify(telemetry).recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.CREDENTIAL_RECONCILIATION,
                OnboardingTelemetryPort.WorkOutcome.RETRY
        );
    }

    @Test
    void activatesTheAccountOnlyAfterCredentialsAndProvisionedResourcesAreReady() {
        when(credentials.getCredentialSetupState(userId))
                .thenReturn(new CredentialSetupState(true, "federico"));
        when(customers.isActive(customerId)).thenReturn(true);
        when(identities.isActive(userId, customerId)).thenReturn(true);
        when(accounts.isActive(accountId)).thenReturn(false, true);

        reconciler.reconcile(item);

        verify(accounts).activate(accountId);
        assertThat(persistedSteps.get(ProvisioningStepType.ACTIVATE_ACCOUNT).status())
                .isEqualTo(com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus.SUCCEEDED);
        assertThat(persistedSteps.get(ProvisioningStepType.ACTIVATE_ACCOUNT).externalReference())
                .isEqualTo(accountId.toString());
        assertThat(persistedApplication.get().status()).isEqualTo(OnboardingApplicationStatus.COMPLETED);
        verify(history).save(any());
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.status() == WorkflowJobStatus.SUCCEEDED
        ));
        verify(telemetry).recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.COMPLETED);
        verify(telemetry).recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.CREDENTIAL_RECONCILIATION,
                OnboardingTelemetryPort.WorkOutcome.SUCCEEDED
        );
    }

    @Test
    void recoversAfterTheAccountWasActivatedBeforeTheStepCouldBeCompleted() {
        when(credentials.getCredentialSetupState(userId))
                .thenReturn(new CredentialSetupState(true, "federico"));
        when(customers.isActive(customerId)).thenReturn(true);
        when(identities.isActive(userId, customerId)).thenReturn(true);
        when(accounts.isActive(accountId)).thenReturn(true);
        persistedSteps.put(ProvisioningStepType.ACTIVATE_ACCOUNT,
                OnboardingProvisioningStep.pending(item.applicationId(), ProvisioningStepType.ACTIVATE_ACCOUNT,
                                NOW.minusSeconds(20))
                        .start(activationFingerprint(accountId), NOW.minusSeconds(10)));

        reconciler.reconcile(item);

        verify(accounts, never()).activate(accountId);
        assertThat(persistedSteps.get(ProvisioningStepType.ACTIVATE_ACCOUNT).status())
                .isEqualTo(com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus.SUCCEEDED);
        assertThat(persistedApplication.get().status()).isEqualTo(OnboardingApplicationStatus.COMPLETED);
    }

    @Test
    void keepsTheActivationStepRetryableAfterATechnicalFailure() {
        persistedSteps.put(ProvisioningStepType.ACTIVATE_ACCOUNT,
                OnboardingProvisioningStep.pending(item.applicationId(), ProvisioningStepType.ACTIVATE_ACCOUNT,
                                NOW.minusSeconds(20))
                        .start(activationFingerprint(accountId), NOW.minusSeconds(10)));

        reconciler.handleTechnicalFailure(item);

        assertThat(persistedSteps.get(ProvisioningStepType.ACTIVATE_ACCOUNT).status())
                .isEqualTo(com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus.RETRY_WAIT);
        assertThat(persistedSteps.get(ProvisioningStepType.ACTIVATE_ACCOUNT).lastErrorCode())
                .isEqualTo("ACCOUNT_ACTIVATION_ERROR");
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.status() == WorkflowJobStatus.RETRY_WAIT
                        && "CREDENTIAL_RECONCILIATION_ERROR".equals(saved.lastErrorCode())
        ));
    }

    @Test
    void expiresFromPersistedTimeWithoutCallingUnavailableExternalSystems() {
        succeeded(item.applicationId(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL, userId,
                NOW.minus(Duration.ofDays(8)));

        reconciler.reconcile(item);

        assertThat(persistedApplication.get().status())
                .isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_EXPIRED);
        verifyNoInteractions(credentials, customers, accounts, identities);
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.status() == WorkflowJobStatus.FAILED
                        && "CREDENTIAL_SETUP_EXPIRED".equals(saved.lastErrorCode())
        ));
    }

    @Test
    void marksASeparatedTechnicalFailureAfterRetriesAreExhausted() {
        OnboardingWorkItem exhausted = item;
        for (int attempt = item.attempts(); attempt < policy.maxAttempts(); attempt++) {
            exhausted = exhausted.claim(NOW.minusSeconds(1), Duration.ofMinutes(5));
        }

        reconciler.handleTechnicalFailure(exhausted);

        assertThat(persistedApplication.get().status())
                .isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_FAILED);
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.status() == WorkflowJobStatus.FAILED
                        && "CREDENTIAL_RECONCILIATION_FAILED".equals(saved.lastErrorCode())
        ));
        verify(telemetry).recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.CREDENTIAL_RECONCILIATION,
                OnboardingTelemetryPort.WorkOutcome.EXHAUSTED
        );
    }

    private void succeeded(UUID applicationId, ProvisioningStepType type, String reference, Instant completedAt) {
        OnboardingProvisioningStep step = OnboardingProvisioningStep.pending(
                applicationId, type, completedAt.minusSeconds(10)
        ).start("request-hash", completedAt.minusSeconds(5)).succeed(reference, completedAt);
        persistedSteps.put(type, step);
    }

    private String activationFingerprint(UUID id) {
        try {
            String request = id + "|AUTO_ONBOARDING_APPROVED|onboarding-service";
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(request.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
