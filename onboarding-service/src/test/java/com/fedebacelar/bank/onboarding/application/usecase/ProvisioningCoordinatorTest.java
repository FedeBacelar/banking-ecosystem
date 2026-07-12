package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.AccountProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CustomerProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.IdentityProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.application.port.out.ProvisioningFailureClassifierPort;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingProvisioningProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class ProvisioningCoordinatorTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingApplicantDataRepositoryPort applicants = mock(OnboardingApplicantDataRepositoryPort.class);
    private final OnboardingProvisioningStepRepositoryPort steps = mock(OnboardingProvisioningStepRepositoryPort.class);
    private final OnboardingStatusHistoryRepositoryPort history = mock(OnboardingStatusHistoryRepositoryPort.class);
    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final CustomerProvisioningPort customers = mock(CustomerProvisioningPort.class);
    private final AccountProvisioningPort accounts = mock(AccountProvisioningPort.class);
    private final CredentialProvisioningPort credentials = mock(CredentialProvisioningPort.class);
    private final IdentityProvisioningPort identities = mock(IdentityProvisioningPort.class);
    private final OnboardingUniquenessReservationPort reservations = mock(OnboardingUniquenessReservationPort.class);
    private final ProvisioningFailureClassifierPort failureClassifier = mock(ProvisioningFailureClassifierPort.class);
    private final OnboardingProvisioningProperties properties = new OnboardingProvisioningProperties();
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final AtomicReference<OnboardingApplication> persistedApplication = new AtomicReference<>();
    private final Map<ProvisioningStepType, OnboardingProvisioningStep> persistedSteps = new EnumMap<>(ProvisioningStepType.class);
    private UUID customerId;
    private UUID accountId;
    private String userId;
    private ProvisioningCoordinator coordinator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(transactions.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
        doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());

        OnboardingApplication application = OnboardingApplication.start(
                "person@example.com", "magic", NOW.plusSeconds(1800), NOW.plus(Duration.ofDays(15)), NOW.minusSeconds(100)
        ).verifyEmail("continuation", NOW.plusSeconds(7200), NOW.minusSeconds(90))
                .submit(NOW.minusSeconds(80))
                .startAutomatedReview(NOW.minusSeconds(70))
                .approve(NOW.minusSeconds(60))
                .startProvisioning(NOW.minusSeconds(50));
        persistedApplication.set(application);
        when(applications.findById(application.id())).thenAnswer(invocation -> Optional.of(persistedApplication.get()));
        when(applications.save(any())).thenAnswer(invocation -> {
            OnboardingApplication saved = invocation.getArgument(0);
            persistedApplication.set(saved);
            return saved;
        });
        when(applicants.findByApplicationId(application.id())).thenReturn(Optional.of(ApplicantData.create(
                application.id(), "Federico", null, "Bacelar", LocalDate.of(1990, 1, 1), "AR",
                ApplicantDocumentType.DNI, "30111222", "AR", LocalDate.of(2030, 1, 1), "+5491111111111",
                "Calle", "1", "Ciudad", "Buenos Aires", "1000", "AR", NOW
        )));
        when(steps.findByApplicationIdAndStepType(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(persistedSteps.get(invocation.getArgument(1))));
        when(steps.findByApplicationId(any())).thenAnswer(invocation -> List.copyOf(persistedSteps.values()));
        when(steps.save(any())).thenAnswer(invocation -> {
            OnboardingProvisioningStep saved = invocation.getArgument(0);
            persistedSteps.put(saved.stepType(), saved);
            return saved;
        });
        when(workItems.findByApplicationIdAndJobType(application.id(), WorkflowJobType.CREDENTIAL_RECONCILIATION))
                .thenReturn(Optional.empty());

        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        userId = UUID.randomUUID().toString();
        when(credentials.precreateUser(any(), any(), any())).thenReturn(userId);
        when(identities.createOrResolve(customerId, userId)).thenReturn(UUID.randomUUID());
        coordinator = new ProvisioningCoordinator(applications, applicants, steps, history, workItems,
                customers, accounts, credentials, identities, reservations, properties, failureClassifier, transactions,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void resumesFromFirstIncompleteStepWithoutDuplicatingCreatedResources() {
        UUID applicationId = persistedApplication.get().id();
        succeeded(applicationId, ProvisioningStepType.CREATE_CUSTOMER, customerId.toString());
        succeeded(applicationId, ProvisioningStepType.APPROVE_CUSTOMER_KYC, customerId.toString());
        succeeded(applicationId, ProvisioningStepType.OPEN_ACCOUNT, accountId.toString());
        OnboardingWorkItem item = OnboardingWorkItem.pending(applicationId, WorkflowJobType.PROVISIONING, NOW.minusSeconds(20))
                .claim(NOW.minusSeconds(10), Duration.ofMinutes(2));

        coordinator.execute(item);

        verify(customers, never()).createCustomer(any(), any(), any());
        verify(customers, never()).approveKyc(any());
        verify(accounts, never()).openDefaultAccount(any(), any());
        verify(credentials).precreateUser(any(), any(), any());
        verify(identities).createOrResolve(customerId, userId);
        verify(accounts, never()).activate(any());
        verify(credentials).sendCredentialSetupEmail(userId);
        assertThat(persistedSteps.values())
                .hasSize(6)
                .allSatisfy(step -> assertThat(step.status()).isEqualTo(ProvisioningStepStatus.SUCCEEDED));
        assertThat(persistedApplication.get().status())
                .isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING);
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                work -> work.jobType() == WorkflowJobType.CREDENTIAL_RECONCILIATION));
        verify(reservations).convertByApplicationId(applicationId, NOW);
    }

    private void succeeded(UUID applicationId, ProvisioningStepType type, String reference) {
        OnboardingProvisioningStep step = OnboardingProvisioningStep.pending(applicationId, type, NOW.minusSeconds(60))
                .start("request-hash", NOW.minusSeconds(55))
                .succeed(reference, NOW.minusSeconds(50));
        persistedSteps.put(type, step);
    }
}
