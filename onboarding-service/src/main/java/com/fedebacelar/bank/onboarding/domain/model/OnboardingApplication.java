package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record OnboardingApplication(
        UUID id,
        String email,
        OnboardingApplicationStatus status,
        String magicLinkTokenHash,
        Instant magicLinkExpiresAt,
        Instant magicLinkConsumedAt,
        Instant emailVerifiedAt,
        String continuationTokenHash,
        Instant continuationExpiresAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    private static final Map<OnboardingApplicationStatus, Set<OnboardingApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(
            OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING, EnumSet.of(OnboardingApplicationStatus.IN_PROGRESS, OnboardingApplicationStatus.EXPIRED),
            OnboardingApplicationStatus.IN_PROGRESS, EnumSet.of(OnboardingApplicationStatus.SUBMITTED, OnboardingApplicationStatus.EXPIRED, OnboardingApplicationStatus.CANCELLED),
            OnboardingApplicationStatus.SUBMITTED, EnumSet.of(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW),
            OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW, EnumSet.of(OnboardingApplicationStatus.APPROVED, OnboardingApplicationStatus.REJECTED),
            OnboardingApplicationStatus.APPROVED, EnumSet.of(OnboardingApplicationStatus.PROVISIONING),
            OnboardingApplicationStatus.PROVISIONING, EnumSet.of(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING, OnboardingApplicationStatus.PROVISIONING_FAILED),
            OnboardingApplicationStatus.PROVISIONING_FAILED, EnumSet.of(OnboardingApplicationStatus.PROVISIONING),
            OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING, EnumSet.of(OnboardingApplicationStatus.COMPLETED)
    );

    public static OnboardingApplication start(
            String email,
            String magicLinkTokenHash,
            Instant magicLinkExpiresAt,
            Instant expiresAt,
            Instant now
    ) {
        return new OnboardingApplication(
                UUID.randomUUID(),
                email,
                OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING,
                magicLinkTokenHash,
                magicLinkExpiresAt,
                null,
                null,
                null,
                null,
                expiresAt,
                now,
                now,
                0L
        );
    }

    public OnboardingApplication verifyEmail(
            String continuationTokenHash,
            Instant continuationExpiresAt,
            Instant now
    ) {
        assertTransition(OnboardingApplicationStatus.IN_PROGRESS);
        return new OnboardingApplication(
                id,
                email,
                OnboardingApplicationStatus.IN_PROGRESS,
                magicLinkTokenHash,
                magicLinkExpiresAt,
                now,
                now,
                continuationTokenHash,
                continuationExpiresAt,
                expiresAt,
                createdAt,
                now,
                version
        );
    }

    public OnboardingApplication expire(Instant now) {
        assertTransition(OnboardingApplicationStatus.EXPIRED);
        return withStatus(OnboardingApplicationStatus.EXPIRED, now);
    }

    public boolean magicLinkConsumed() {
        return magicLinkConsumedAt != null;
    }

    public boolean magicLinkExpired(Instant now) {
        return !magicLinkExpiresAt.isAfter(now);
    }

    public boolean continuationExpired(Instant now) {
        return continuationExpiresAt == null || !continuationExpiresAt.isAfter(now);
    }

    public boolean activeForDuplicateCheck() {
        return !EnumSet.of(
                OnboardingApplicationStatus.COMPLETED,
                OnboardingApplicationStatus.REJECTED,
                OnboardingApplicationStatus.EXPIRED,
                OnboardingApplicationStatus.CANCELLED
        ).contains(status);
    }

    private OnboardingApplication withStatus(OnboardingApplicationStatus newStatus, Instant now) {
        return new OnboardingApplication(
                id,
                email,
                newStatus,
                magicLinkTokenHash,
                magicLinkExpiresAt,
                magicLinkConsumedAt,
                emailVerifiedAt,
                continuationTokenHash,
                continuationExpiresAt,
                expiresAt,
                createdAt,
                now,
                version
        );
    }

    private void assertTransition(OnboardingApplicationStatus requestedStatus) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(status, Set.of()).contains(requestedStatus)) {
            throw new InvalidOnboardingStatusTransitionException(status, requestedStatus);
        }
    }
}
