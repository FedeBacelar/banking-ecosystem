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
public class ProvisioningWorker {
    private final OnboardingWorkItemRepositoryPort workItems;
    private final ProvisioningCoordinator coordinator;
    private final OnboardingProvisioningPolicyPort provisioningPolicy;
    private final Clock clock;
    private final OnboardingTelemetryPort telemetry;
    public ProvisioningWorker(OnboardingWorkItemRepositoryPort workItems, ProvisioningCoordinator coordinator,
            OnboardingProvisioningPolicyPort provisioningPolicy, OnboardingTelemetryPort telemetry, Clock clock) {
        this.workItems = workItems; this.coordinator = coordinator; this.provisioningPolicy = provisioningPolicy;
        this.telemetry = telemetry; this.clock = clock;
    }
    @Scheduled(fixedDelayString = "${onboarding.provisioning.worker-delay:PT1S}")
    public void processDueProvisioning() {
        for (int processed = 0; processed < provisioningPolicy.workerBatchSize(); processed++) {
            OnboardingWorkItem item = workItems.claimNext(WorkflowJobType.PROVISIONING, Instant.now(clock), provisioningPolicy.workerLease()).orElse(null);
            if (item == null) return;
            telemetry.observeWorkerExecution(OnboardingTelemetryPort.WorkType.PROVISIONING, () -> {
                telemetry.recordWorkClaimed(OnboardingTelemetryPort.WorkType.PROVISIONING);
                try { coordinator.execute(item); }
                catch (RuntimeException exception) { coordinator.handleFailure(item, exception); }
            });
        }
    }
}
