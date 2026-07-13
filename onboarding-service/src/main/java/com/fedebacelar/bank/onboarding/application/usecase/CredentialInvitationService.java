package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialInvitationCooldownException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidIdempotencyKeyException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CredentialInvitationService implements ResendCredentialInvitationUseCase {
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final OnboardingApplicationRepositoryPort applications;
    private final OnboardingProvisioningStepRepositoryPort steps;
    private final CredentialInvitationDeliveryRepositoryPort deliveries;
    private final CredentialInvitationDeliveryPolicyPort policy;
    private final TokenHashingPort tokenHashing;
    private final Clock clock;

    public CredentialInvitationService(
            OnboardingApplicationRepositoryPort applications,
            OnboardingProvisioningStepRepositoryPort steps,
            CredentialInvitationDeliveryRepositoryPort deliveries,
            CredentialInvitationDeliveryPolicyPort policy,
            TokenHashingPort tokenHashing,
            Clock clock
    ) {
        this.applications = applications;
        this.steps = steps;
        this.deliveries = deliveries;
        this.policy = policy;
        this.tokenHashing = tokenHashing;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OnboardingSubmissionDetails resend(String continuationToken, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        Instant now = Instant.now(clock);
        OnboardingApplication application = applications.findByContinuationTokenHashForUpdate(
                        tokenHashing.hash(continuationToken)
                )
                .orElseThrow(InvalidContinuationTokenException::new);
        if (application.continuationExpired(now)) {
            throw new OnboardingContinuationExpiredException();
        }

        String idempotencyKeyHash = tokenHashing.hash(idempotencyKey);
        if (deliveries.findByApplicationIdAndIdempotencyKeyHash(
                application.id(), idempotencyKeyHash
        ).isPresent()) {
            return details(application);
        }

        if (application.status() != OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING) {
            throw new InvalidOnboardingStatusTransitionException(
                    application.status(), OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING
            );
        }

        OnboardingProvisioningStep invitationStep = steps.findByApplicationIdAndStepType(
                        application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL
                )
                .filter(step -> step.status() == ProvisioningStepStatus.SUCCEEDED)
                .filter(step -> step.externalReference() != null && !step.externalReference().isBlank())
                .orElseThrow(() -> new IllegalStateException("Credential invitation provisioning step is not available."));

        var activeDeliveries = deliveries.findActiveByApplicationIdForUpdate(application.id());
        Instant latestAcceptedAt = deliveries.findLatestByApplicationId(application.id())
                .map(CredentialInvitationDelivery::createdAt)
                .filter(createdAt -> createdAt.isAfter(invitationStep.updatedAt()))
                .orElse(invitationStep.updatedAt());
        Instant cooldownEndsAt = latestAcceptedAt.plus(policy.credentialInvitationCooldown());
        if (cooldownEndsAt.isAfter(now)) {
            throw new CredentialInvitationCooldownException(retryAfterSeconds(now, cooldownEndsAt));
        }

        activeDeliveries.stream()
                .filter(delivery -> delivery.status() == WorkflowJobStatus.RUNNING)
                .map(CredentialInvitationDelivery::lockedUntil)
                .filter(lockedUntil -> lockedUntil != null && lockedUntil.isAfter(now))
                .findFirst()
                .ifPresent(lockedUntil -> {
                    throw new CredentialInvitationCooldownException(retryAfterSeconds(now, lockedUntil));
                });
        activeDeliveries.stream()
                .map(delivery -> delivery.fail("CREDENTIAL_INVITATION_SUPERSEDED", now))
                .forEach(deliveries::save);

        deliveries.save(CredentialInvitationDelivery.pending(application.id(), idempotencyKeyHash, now));
        return details(application);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException();
        }
    }

    private long retryAfterSeconds(Instant now, Instant cooldownEndsAt) {
        long remainingMillis = Duration.between(now, cooldownEndsAt).toMillis();
        return Math.max(1L, (remainingMillis + 999L) / 1_000L);
    }

    private OnboardingSubmissionDetails details(OnboardingApplication application) {
        return new OnboardingSubmissionDetails(
                application.id(), application.status(), application.submittedAt(), application.updatedAt()
        );
    }
}
