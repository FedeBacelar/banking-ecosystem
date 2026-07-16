package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningPolicyPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CredentialReconciliationWorker {
    private final OnboardingWorkItemRepositoryPort workItems;
    private final CredentialSetupReconciler reconciler;
    private final OnboardingProvisioningPolicyPort provisioningPolicy;
    private final Clock clock;
    private final OnboardingTelemetryPort telemetry;
    public CredentialReconciliationWorker(OnboardingWorkItemRepositoryPort workItems, CredentialSetupReconciler reconciler,
            OnboardingProvisioningPolicyPort provisioningPolicy, OnboardingTelemetryPort telemetry, Clock clock) {
        this.workItems = workItems; this.reconciler = reconciler; this.provisioningPolicy = provisioningPolicy;
        this.telemetry = telemetry; this.clock = clock;
    }
    @Scheduled(fixedDelayString = "${onboarding.provisioning.credential-reconciliation-delay:PT30S}")
    public void reconcileCredentials() {
        for (int processed = 0; processed < provisioningPolicy.workerBatchSize(); processed++) {
            OnboardingWorkItem item = workItems.claimNext(WorkflowJobType.CREDENTIAL_RECONCILIATION,
                    Instant.now(clock), provisioningPolicy.workerLease()).orElse(null);
            if (item == null) return;
            telemetry.observeWorkerExecution(OnboardingTelemetryPort.WorkType.CREDENTIAL_RECONCILIATION, () -> {
                telemetry.recordWorkClaimed(OnboardingTelemetryPort.WorkType.CREDENTIAL_RECONCILIATION);
                try { reconciler.reconcile(item); }
                catch (RuntimeException exception) { reconciler.handleTechnicalFailure(item); }
            });
        }
    }
}
