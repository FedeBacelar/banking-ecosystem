package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!observability")
public class NoOpOnboardingTelemetryAdapter implements OnboardingTelemetryPort {

    @Override
    public void recordApplicationEvent(ApplicationEvent event) {
        // Functional telemetry is opt-in.
    }

    @Override
    public void recordWorkClaimed(WorkType workType) {
        // Functional telemetry is opt-in.
    }

    @Override
    public void recordWorkOutcome(WorkType workType, WorkOutcome outcome) {
        // Functional telemetry is opt-in.
    }

    @Override
    public void observeWorkerExecution(WorkType workType, Runnable execution) {
        execution.run();
    }

    @Override
    public void observeExpirationExecution(Runnable execution) {
        execution.run();
    }

    @Override
    public <T> T observeProvisioningStep(ProvisioningStepType stepType, Supplier<T> execution) {
        return execution.get();
    }
}
