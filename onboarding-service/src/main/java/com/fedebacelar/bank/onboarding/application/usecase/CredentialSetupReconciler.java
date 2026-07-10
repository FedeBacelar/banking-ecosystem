package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.model.CredentialSetupState;
import com.fedebacelar.bank.onboarding.application.port.out.AccountProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CustomerProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.IdentityProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingProvisioningProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CredentialSetupReconciler {
    private final OnboardingApplicationRepositoryPort applications;
    private final OnboardingProvisioningStepRepositoryPort steps;
    private final OnboardingStatusHistoryRepositoryPort history;
    private final OnboardingWorkItemRepositoryPort workItems;
    private final CredentialProvisioningPort credentials;
    private final CustomerProvisioningPort customers;
    private final AccountProvisioningPort accounts;
    private final IdentityProvisioningPort identities;
    private final OnboardingUniquenessReservationPort reservations;
    private final OnboardingProvisioningProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public CredentialSetupReconciler(OnboardingApplicationRepositoryPort applications,
            OnboardingProvisioningStepRepositoryPort steps, OnboardingStatusHistoryRepositoryPort history,
            OnboardingWorkItemRepositoryPort workItems, CredentialProvisioningPort credentials,
            CustomerProvisioningPort customers, AccountProvisioningPort accounts, IdentityProvisioningPort identities,
            OnboardingUniquenessReservationPort reservations,
            OnboardingProvisioningProperties properties, TransactionTemplate transactionTemplate, Clock clock) {
        this.applications = applications; this.steps = steps; this.history = history; this.workItems = workItems;
        this.credentials = credentials; this.customers = customers; this.accounts = accounts; this.identities = identities;
        this.reservations = reservations;
        this.properties = properties; this.transactionTemplate = transactionTemplate; this.clock = clock;
    }

    public void reconcile(OnboardingWorkItem item) {
        OnboardingApplication application = applications.findById(item.applicationId())
                .orElseThrow(() -> new OnboardingApplicationNotFoundException(item.applicationId()));
        if (application.status() != OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
            workItems.save(item.succeed(Instant.now(clock)));
            return;
        }
        UUID customerId = uuidReference(application.id(), ProvisioningStepType.CREATE_CUSTOMER);
        UUID accountId = uuidReference(application.id(), ProvisioningStepType.OPEN_ACCOUNT);
        String userId = reference(application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER);
        CredentialSetupState state = credentials.getCredentialSetupState(userId);

        if (!state.complete()) {
            reschedule(item, "CREDENTIAL_SETUP_PENDING", Duration.ofSeconds(30));
            return;
        }
        if (!customers.isActive(customerId) || !accounts.isActive(accountId) || !identities.isActive(userId, customerId)) {
            reschedule(item, "PROVISIONED_RESOURCES_NOT_ACTIVE", Duration.ofSeconds(30));
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.now(clock);
            OnboardingApplication current = applications.findById(item.applicationId())
                    .orElseThrow(() -> new OnboardingApplicationNotFoundException(item.applicationId()));
            if (current.status() == OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
                OnboardingApplication completed = applications.save(current.complete(now));
                history.save(OnboardingStatusHistory.transition(current.id(), current.status(), completed.status(),
                        "CREDENTIAL_SETUP_COMPLETED", OnboardingActorType.CREDENTIAL_RECONCILIATION, now));
                reservations.convertByApplicationId(current.id(), now);
            }
            workItems.save(item.succeed(now));
        });
    }

    public void handleTechnicalFailure(OnboardingWorkItem item) {
        reschedule(item, "CREDENTIAL_RECONCILIATION_ERROR", properties.retryDelay(item.attempts()));
    }

    private void reschedule(OnboardingWorkItem item, String code, Duration delay) {
        Instant now = Instant.now(clock);
        workItems.save(item.retry(code, now.plus(delay), now));
    }
    private UUID uuidReference(UUID applicationId, ProvisioningStepType type) { return UUID.fromString(reference(applicationId, type)); }
    private String reference(UUID applicationId, ProvisioningStepType type) {
        return steps.findByApplicationIdAndStepType(applicationId, type)
                .filter(step -> step.status() == ProvisioningStepStatus.SUCCEEDED)
                .map(OnboardingProvisioningStep::externalReference)
                .orElseThrow(() -> new IllegalStateException("Provisioning reference is missing: " + type));
    }
}
