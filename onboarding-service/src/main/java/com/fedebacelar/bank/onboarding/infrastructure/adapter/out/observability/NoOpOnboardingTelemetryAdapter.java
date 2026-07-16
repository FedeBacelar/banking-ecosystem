package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
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
}
