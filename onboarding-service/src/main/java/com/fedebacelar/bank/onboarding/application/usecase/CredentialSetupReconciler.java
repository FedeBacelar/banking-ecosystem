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
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningPolicyPort;
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
    private final OnboardingProvisioningPolicyPort provisioningPolicy;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public CredentialSetupReconciler(OnboardingApplicationRepositoryPort applications,
            OnboardingProvisioningStepRepositoryPort steps, OnboardingStatusHistoryRepositoryPort history,
            OnboardingWorkItemRepositoryPort workItems, CredentialProvisioningPort credentials,
            CustomerProvisioningPort customers, AccountProvisioningPort accounts, IdentityProvisioningPort identities,
            OnboardingProvisioningPolicyPort provisioningPolicy, TransactionTemplate transactionTemplate, Clock clock) {
        this.applications = applications; this.steps = steps; this.history = history; this.workItems = workItems;
        this.credentials = credentials; this.customers = customers; this.accounts = accounts; this.identities = identities;
        this.provisioningPolicy = provisioningPolicy; this.transactionTemplate = transactionTemplate; this.clock = clock;
    }

    public void reconcile(OnboardingWorkItem item) {
        OnboardingApplication application = applications.findById(item.applicationId())
                .orElseThrow(() -> new OnboardingApplicationNotFoundException(item.applicationId()));
        if (application.status() != OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
            workItems.save(item.succeed(Instant.now(clock)));
            return;
        }
        OnboardingProvisioningStep invitation = steps.findByApplicationIdAndStepType(
                application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL
        ).orElseThrow(() -> new IllegalStateException("Credential invitation step is missing."));
        Instant deadline = invitation.completedAt().plus(provisioningPolicy.credentialSetupTimeout());
        if (!Instant.now(clock).isBefore(deadline)) {
            expireCredentialSetup(item);
            return;
        }

        UUID customerId = uuidReference(application.id(), ProvisioningStepType.CREATE_CUSTOMER);
        UUID accountId = uuidReference(application.id(), ProvisioningStepType.OPEN_ACCOUNT);
        String userId = reference(application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER);
        CredentialSetupState state = credentials.getCredentialSetupState(userId);

        if (!state.complete()) {
            waitForApplicant(item, "CREDENTIAL_SETUP_PENDING");
            return;
        }
        if (!customers.isActive(customerId) || !identities.isActive(userId, customerId)) {
            waitForApplicant(item, "PROVISIONED_RESOURCES_NOT_ACTIVE");
            return;
        }
        if (!accounts.isActive(accountId)) {
            accounts.activate(accountId);
        }
        if (!accounts.isActive(accountId)) {
            waitForApplicant(item, "ACCOUNT_ACTIVATION_PENDING");
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
            }
            workItems.save(item.succeed(now));
        });
    }

    public void handleTechnicalFailure(OnboardingWorkItem item) {
        if (item.attempts() >= provisioningPolicy.maxAttempts()) {
            failCredentialSetup(item);
            return;
        }
        reschedule(item, "CREDENTIAL_RECONCILIATION_ERROR", provisioningPolicy.retryDelay(item.attempts()));
    }

    private void waitForApplicant(OnboardingWorkItem item, String code) {
        Instant now = Instant.now(clock);
        workItems.save(item.waitUntil(code, now.plus(provisioningPolicy.credentialReconciliationDelay()), now));
    }

    private void expireCredentialSetup(OnboardingWorkItem item) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.now(clock);
            OnboardingApplication current = applications.findById(item.applicationId())
                    .orElseThrow(() -> new OnboardingApplicationNotFoundException(item.applicationId()));
            if (current.status() == OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
                OnboardingApplication expired = applications.save(current.expireCredentialSetup(now));
                history.save(OnboardingStatusHistory.transition(
                        current.id(), current.status(), expired.status(), "CREDENTIAL_SETUP_EXPIRED",
                        OnboardingActorType.CREDENTIAL_RECONCILIATION, now
                ));
            }
            workItems.save(item.fail("CREDENTIAL_SETUP_EXPIRED", now));
        });
    }

    private void failCredentialSetup(OnboardingWorkItem item) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.now(clock);
            OnboardingApplication current = applications.findById(item.applicationId())
                    .orElseThrow(() -> new OnboardingApplicationNotFoundException(item.applicationId()));
            if (current.status() == OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
                OnboardingApplication failed = applications.save(current.failCredentialSetup(now));
                history.save(OnboardingStatusHistory.transition(
                        current.id(), current.status(), failed.status(), "CREDENTIAL_SETUP_TECHNICAL_FAILURE",
                        OnboardingActorType.CREDENTIAL_RECONCILIATION, now
                ));
            }
            workItems.save(item.fail("CREDENTIAL_RECONCILIATION_FAILED", now));
        });
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
