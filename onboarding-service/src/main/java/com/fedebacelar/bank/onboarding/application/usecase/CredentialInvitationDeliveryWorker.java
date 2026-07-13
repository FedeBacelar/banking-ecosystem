package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CredentialInvitationDeliveryWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialInvitationDeliveryWorker.class);

    private final CredentialInvitationDeliveryRepositoryPort deliveries;
    private final OnboardingApplicationRepositoryPort applications;
    private final OnboardingProvisioningStepRepositoryPort steps;
    private final CredentialProvisioningPort credentials;
    private final CredentialInvitationDeliveryPolicyPort policy;
    private final Clock clock;

    public CredentialInvitationDeliveryWorker(
            CredentialInvitationDeliveryRepositoryPort deliveries,
            OnboardingApplicationRepositoryPort applications,
            OnboardingProvisioningStepRepositoryPort steps,
            CredentialProvisioningPort credentials,
            CredentialInvitationDeliveryPolicyPort policy,
            Clock clock
    ) {
        this.deliveries = deliveries;
        this.applications = applications;
        this.steps = steps;
        this.credentials = credentials;
        this.policy = policy;
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
            deliver(delivery);
        }
    }

    private void deliver(CredentialInvitationDelivery delivery) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = applications.findById(delivery.applicationId()).orElse(null);
        if (application == null || application.status() != OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
            deliveries.save(delivery.fail("CREDENTIAL_INVITATION_NOT_APPLICABLE", now));
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
            return;
        }

        try {
            credentials.sendCredentialSetupEmail(invitationStep.externalReference());
            deliveries.save(delivery.succeed(Instant.now(clock)));
        } catch (RuntimeException exception) {
            handleFailure(delivery, exception, Instant.now(clock));
        }
    }

    private void handleFailure(
            CredentialInvitationDelivery delivery,
            RuntimeException exception,
            Instant now
    ) {
        LOGGER.warn(
                "Credential invitation delivery failed for deliveryId={} applicationId={} errorType={}",
                delivery.id(), delivery.applicationId(), exception.getClass().getSimpleName()
        );
        if (delivery.attempts() >= policy.maxAttempts()) {
            deliveries.save(delivery.fail("CREDENTIAL_INVITATION_DELIVERY_FAILED", now));
            return;
        }
        deliveries.save(delivery.retry(
                "CREDENTIAL_INVITATION_DELIVERY_RETRY",
                now.plus(policy.retryDelay(delivery.attempts())),
                now
        ));
    }
}
