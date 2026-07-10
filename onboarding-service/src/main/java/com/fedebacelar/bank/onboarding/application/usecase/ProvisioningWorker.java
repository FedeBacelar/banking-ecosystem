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
public class ProvisioningWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningWorker.class);
    private final OnboardingWorkItemRepositoryPort workItems;
    private final ProvisioningCoordinator coordinator;
    private final OnboardingProvisioningProperties properties;
    private final Clock clock;
    public ProvisioningWorker(OnboardingWorkItemRepositoryPort workItems, ProvisioningCoordinator coordinator,
            OnboardingProvisioningProperties properties, Clock clock) {
        this.workItems = workItems; this.coordinator = coordinator; this.properties = properties; this.clock = clock;
    }
    @Scheduled(fixedDelayString = "${onboarding.provisioning.worker-delay:PT1S}")
    public void processDueProvisioning() {
        for (int processed = 0; processed < properties.getWorkerBatchSize(); processed++) {
            OnboardingWorkItem item = workItems.claimNext(WorkflowJobType.PROVISIONING, Instant.now(clock), properties.getWorkerLease()).orElse(null);
            if (item == null) return;
            try { coordinator.execute(item); }
            catch (RuntimeException exception) {
                LOGGER.warn("Provisioning failed for applicationId={} errorType={}", item.applicationId(), exception.getClass().getSimpleName());
                coordinator.handleFailure(item, exception);
            }
        }
    }
}
