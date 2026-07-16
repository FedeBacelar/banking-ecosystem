package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CredentialInvitationDeliveryWorker {
    private final CredentialInvitationDeliveryRepositoryPort deliveries;
    private final OnboardingApplicationRepositoryPort applications;
    private final OnboardingProvisioningStepRepositoryPort steps;
    private final CredentialProvisioningPort credentials;
    private final CredentialInvitationDeliveryPolicyPort policy;
    private final Clock clock;
    private final OnboardingTelemetryPort telemetry;

    public CredentialInvitationDeliveryWorker(
            CredentialInvitationDeliveryRepositoryPort deliveries,
            OnboardingApplicationRepositoryPort applications,
            OnboardingProvisioningStepRepositoryPort steps,
            CredentialProvisioningPort credentials,
            CredentialInvitationDeliveryPolicyPort policy,
            OnboardingTelemetryPort telemetry,
            Clock clock
    ) {
        this.deliveries = deliveries;
        this.applications = applications;
        this.steps = steps;
        this.credentials = credentials;
        this.policy = policy;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${onboarding.provisioning.credential-invitation-worker-delay:PT1S}")
    public void processPendingDeliveries() {
        for (int processed = 0; processed < policy.workerBatchSize(); processed++) {
            CredentialInvitationDelivery delivery = deliveries.claimNext(
                    Instant.now(clock), policy.workerLease()
            ).orElse(null);
            if (delivery == null) {
                return;
            }
            telemetry.observeWorkerExecution(
                    OnboardingTelemetryPort.WorkType.CREDENTIAL_INVITATION_DELIVERY,
                    () -> {
                        telemetry.recordWorkClaimed(
                                OnboardingTelemetryPort.WorkType.CREDENTIAL_INVITATION_DELIVERY
                        );
                        deliver(delivery);
                    }
            );
        }
    }

    private void deliver(CredentialInvitationDelivery delivery) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = applications.findById(delivery.applicationId()).orElse(null);
        if (application == null || application.status() != OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
            deliveries.save(delivery.fail("CREDENTIAL_INVITATION_NOT_APPLICABLE", now));
            recordOutcome(OnboardingTelemetryPort.WorkOutcome.EXHAUSTED);
            return;
        }

        OnboardingProvisioningStep invitationStep = steps.findByApplicationIdAndStepType(
                delivery.applicationId(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL
        ).orElse(null);
        if (invitationStep == null
                || invitationStep.status() != ProvisioningStepStatus.SUCCEEDED
                || invitationStep.externalReference() == null
                || invitationStep.externalReference().isBlank()) {
            deliveries.save(delivery.fail("CREDENTIAL_INVITATION_NOT_AVAILABLE", now));
            recordOutcome(OnboardingTelemetryPort.WorkOutcome.EXHAUSTED);
            return;
        }

        try {
            credentials.sendCredentialSetupEmail(invitationStep.externalReference());
            deliveries.save(delivery.succeed(Instant.now(clock)));
            recordOutcome(OnboardingTelemetryPort.WorkOutcome.SUCCEEDED);
        } catch (RuntimeException exception) {
            handleFailure(delivery, exception, Instant.now(clock));
        }
    }

    private void handleFailure(
            CredentialInvitationDelivery delivery,
            RuntimeException exception,
            Instant now
    ) {
        if (delivery.attempts() >= policy.maxAttempts()) {
            deliveries.save(delivery.fail("CREDENTIAL_INVITATION_DELIVERY_FAILED", now));
            recordOutcome(OnboardingTelemetryPort.WorkOutcome.EXHAUSTED);
            return;
        }
        deliveries.save(delivery.retry(
                "CREDENTIAL_INVITATION_DELIVERY_RETRY",
                now.plus(policy.retryDelay(delivery.attempts())),
                now
        ));
        recordOutcome(OnboardingTelemetryPort.WorkOutcome.RETRY);
    }

    private void recordOutcome(OnboardingTelemetryPort.WorkOutcome outcome) {
        telemetry.recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.CREDENTIAL_INVITATION_DELIVERY,
                outcome
        );
    }
}
