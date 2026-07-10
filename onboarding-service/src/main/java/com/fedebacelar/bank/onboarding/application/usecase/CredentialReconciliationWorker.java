package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingProvisioningProperties;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CredentialReconciliationWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialReconciliationWorker.class);
    private final OnboardingWorkItemRepositoryPort workItems;
    private final CredentialSetupReconciler reconciler;
    private final OnboardingProvisioningProperties properties;
    private final Clock clock;
    public CredentialReconciliationWorker(OnboardingWorkItemRepositoryPort workItems, CredentialSetupReconciler reconciler,
            OnboardingProvisioningProperties properties, Clock clock) {
        this.workItems = workItems; this.reconciler = reconciler; this.properties = properties; this.clock = clock;
    }
    @Scheduled(fixedDelayString = "${onboarding.provisioning.credential-reconciliation-delay:PT30S}")
    public void reconcileCredentials() {
        for (int processed = 0; processed < properties.getWorkerBatchSize(); processed++) {
            OnboardingWorkItem item = workItems.claimNext(WorkflowJobType.CREDENTIAL_RECONCILIATION,
                    Instant.now(clock), properties.getWorkerLease()).orElse(null);
            if (item == null) return;
            try { reconciler.reconcile(item); }
            catch (RuntimeException exception) {
                LOGGER.warn("Credential reconciliation failed for applicationId={} errorType={}", item.applicationId(), exception.getClass().getSimpleName());
                reconciler.handleTechnicalFailure(item);
            }
        }
    }
}
