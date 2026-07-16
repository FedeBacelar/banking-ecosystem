package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import java.util.Locale;
import java.util.function.Supplier;

public interface OnboardingTelemetryPort {

    enum ApplicationEvent {
        CREATED,
        SUBMITTED,
        COMPLETED,
        REJECTED;

        public String metricValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    enum WorkType {
        MAGIC_LINK_DELIVERY,
        AUTO_REVIEW,
        PROVISIONING,
        CREDENTIAL_RECONCILIATION,
        CREDENTIAL_INVITATION_DELIVERY;

        public static WorkType from(WorkflowJobType jobType) {
            return valueOf(jobType.name());
        }

        public String metricValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    enum WorkOutcome {
        SUCCEEDED,
        RETRY,
        EXHAUSTED;

        public String metricValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    void recordApplicationEvent(ApplicationEvent event);

    void recordWorkClaimed(WorkType workType);

    void recordWorkOutcome(WorkType workType, WorkOutcome outcome);

    void observeWorkerExecution(WorkType workType, Runnable execution);

    void observeExpirationExecution(Runnable execution);

    <T> T observeProvisioningStep(ProvisioningStepType stepType, Supplier<T> execution);
}
