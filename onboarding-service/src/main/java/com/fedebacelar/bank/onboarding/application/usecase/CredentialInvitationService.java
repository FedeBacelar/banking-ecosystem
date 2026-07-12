package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialInvitationCooldownException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CredentialInvitationService implements ResendCredentialInvitationUseCase {
    private final OnboardingApplicationRepositoryPort applications;
    private final OnboardingProvisioningStepRepositoryPort steps;
    private final CredentialProvisioningPort credentials;
    private final TokenHashingPort tokenHashing;
    private final Clock clock;
    private final Duration cooldown;

    public CredentialInvitationService(OnboardingApplicationRepositoryPort applications,
            OnboardingProvisioningStepRepositoryPort steps, CredentialProvisioningPort credentials,
            TokenHashingPort tokenHashing, Clock clock,
            @Value("${onboarding.provisioning.credential-invitation-cooldown:PT1M}") Duration cooldown) {
        this.applications = applications; this.steps = steps; this.credentials = credentials;
        this.tokenHashing = tokenHashing; this.clock = clock; this.cooldown = cooldown;
    }

    @Override
    public OnboardingSubmissionDetails resend(String continuationToken) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = applications.findByContinuationTokenHash(tokenHashing.hash(continuationToken))
                .orElseThrow(InvalidContinuationTokenException::new);
        if (application.continuationExpired(now)) throw new OnboardingContinuationExpiredException();
        if (application.status() != OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
            throw new InvalidOnboardingStatusTransitionException(application.status(), OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING);
        }
        OnboardingProvisioningStep step = steps.findByApplicationIdAndStepType(application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL)
                .orElseThrow(() -> new IllegalStateException("Credential invitation provisioning step is missing."));
        if (step.status() != ProvisioningStepStatus.SUCCEEDED && step.status() != ProvisioningStepStatus.RUNNING) {
            throw new IllegalStateException("Credential invitation provisioning step is not retryable.");
        }
        if (step.updatedAt().plus(cooldown).isAfter(now)) {
            throw new CredentialInvitationCooldownException();
        }
        if (step.status() == ProvisioningStepStatus.RUNNING) {
            step = steps.save(step.restoreAfterInvitationFailure("STALE_INVITATION_ATTEMPT", now));
        }

        OnboardingProvisioningStep running = steps.save(step.start(step.requestHash(), now));
        try {
            credentials.sendCredentialSetupEmail(running.externalReference());
            steps.save(running.succeed(running.externalReference(), Instant.now(clock)));
        } catch (RuntimeException exception) {
            steps.save(running.restoreAfterInvitationFailure("CREDENTIAL_INVITATION_DELIVERY_ERROR", Instant.now(clock)));
            throw exception;
        }
        return new OnboardingSubmissionDetails(application.id(), application.status(), application.submittedAt(), application.updatedAt());
    }
}
