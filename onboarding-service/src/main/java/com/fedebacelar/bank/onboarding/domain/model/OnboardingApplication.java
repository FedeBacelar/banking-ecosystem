package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingReviewMode;
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
        OnboardingReviewMode reviewMode,
        String reviewPolicyVersion,
        String magicLinkTokenHash,
        Instant magicLinkExpiresAt,
        Instant magicLinkConsumedAt,
        Instant emailVerifiedAt,
        String continuationTokenHash,
        Instant continuationExpiresAt,
        Instant expiresAt,
        Instant submittedAt,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public static final String DEFAULT_REVIEW_POLICY_VERSION = "AR_DNI_SAVINGS_V1";

    private static final Map<OnboardingApplicationStatus, Set<OnboardingApplicationStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING, EnumSet.of(OnboardingApplicationStatus.IN_PROGRESS, OnboardingApplicationStatus.EXPIRED)),
            Map.entry(OnboardingApplicationStatus.IN_PROGRESS, EnumSet.of(OnboardingApplicationStatus.SUBMITTED, OnboardingApplicationStatus.EXPIRED, OnboardingApplicationStatus.CANCELLED)),
            Map.entry(OnboardingApplicationStatus.SUBMITTED, EnumSet.of(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW, OnboardingApplicationStatus.EXPIRED)),
            Map.entry(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW, EnumSet.of(OnboardingApplicationStatus.APPROVED, OnboardingApplicationStatus.REJECTED, OnboardingApplicationStatus.REVIEW_FAILED, OnboardingApplicationStatus.EXPIRED)),
            Map.entry(OnboardingApplicationStatus.REVIEW_FAILED, EnumSet.of(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW, OnboardingApplicationStatus.EXPIRED)),
            Map.entry(OnboardingApplicationStatus.APPROVED, EnumSet.of(OnboardingApplicationStatus.PROVISIONING)),
            Map.entry(OnboardingApplicationStatus.PROVISIONING, EnumSet.of(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING, OnboardingApplicationStatus.PROVISIONING_FAILED)),
            Map.entry(OnboardingApplicationStatus.PROVISIONING_FAILED, EnumSet.of(OnboardingApplicationStatus.PROVISIONING)),
            Map.entry(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING, EnumSet.of(OnboardingApplicationStatus.COMPLETED))
    );

    public OnboardingApplication(
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
        this(id, email, status, OnboardingReviewMode.AUTO, DEFAULT_REVIEW_POLICY_VERSION,
                magicLinkTokenHash, magicLinkExpiresAt, magicLinkConsumedAt, emailVerifiedAt,
                continuationTokenHash, continuationExpiresAt, expiresAt, null, null, createdAt, updatedAt, version);
    }

    public static OnboardingApplication start(
            String email,
            String magicLinkTokenHash,
            Instant magicLinkExpiresAt,
            Instant expiresAt,
            Instant now
    ) {
        return start(email, magicLinkTokenHash, magicLinkExpiresAt, expiresAt,
                OnboardingReviewMode.AUTO, DEFAULT_REVIEW_POLICY_VERSION, now);
    }

    public static OnboardingApplication start(
            String email,
            String magicLinkTokenHash,
            Instant magicLinkExpiresAt,
            Instant expiresAt,
            OnboardingReviewMode reviewMode,
            String reviewPolicyVersion,
            Instant now
    ) {
        return new OnboardingApplication(
                UUID.randomUUID(), email, OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING,
                reviewMode, reviewPolicyVersion,
                magicLinkTokenHash, magicLinkExpiresAt, null, null, null, null, expiresAt,
                null, null, now, now, 0L
        );
    }

    public OnboardingApplication verifyEmail(String continuationTokenHash, Instant continuationExpiresAt, Instant now) {
        assertTransition(OnboardingApplicationStatus.IN_PROGRESS);
        return copy(OnboardingApplicationStatus.IN_PROGRESS, magicLinkTokenHash, magicLinkExpiresAt, now, now,
                continuationTokenHash, continuationExpiresAt, submittedAt, decidedAt, now);
    }

    public OnboardingApplication refreshMagicLink(String newTokenHash, Instant newExpiresAt, Instant now) {
        if (status != OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING) {
            throw new InvalidOnboardingStatusTransitionException(status, OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING);
        }
        return copy(status, newTokenHash, newExpiresAt, null, null, null, null, submittedAt, decidedAt, now);
    }

    public OnboardingApplication refreshAccessLink(String newTokenHash, Instant newExpiresAt, Instant now) {
        if (!activeForDuplicateCheck()) {
            throw new InvalidOnboardingStatusTransitionException(status, OnboardingApplicationStatus.IN_PROGRESS);
        }
        return copy(status, newTokenHash, newExpiresAt, null, emailVerifiedAt,
                continuationTokenHash, continuationExpiresAt, submittedAt, decidedAt, now);
    }

    public OnboardingApplication renewContinuation(String newTokenHash, Instant newExpiresAt, Instant now) {
        if (!activeForDuplicateCheck()) {
            throw new InvalidOnboardingStatusTransitionException(status, OnboardingApplicationStatus.IN_PROGRESS);
        }
        return copy(status, magicLinkTokenHash, magicLinkExpiresAt, now, emailVerifiedAt,
                newTokenHash, newExpiresAt, submittedAt, decidedAt, now);
    }

    public OnboardingApplication expire(Instant now) {
        assertTransition(OnboardingApplicationStatus.EXPIRED);
        return withStatus(OnboardingApplicationStatus.EXPIRED, now, submittedAt, decidedAt);
    }

    public OnboardingApplication submit(Instant now) {
        assertTransition(OnboardingApplicationStatus.SUBMITTED);
        return withStatus(OnboardingApplicationStatus.SUBMITTED, now, now, decidedAt);
    }

    public OnboardingApplication startAutomatedReview(Instant now) {
        assertTransition(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW);
        return withStatus(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW, now, submittedAt, decidedAt);
    }

    public OnboardingApplication approve(Instant now) {
        assertTransition(OnboardingApplicationStatus.APPROVED);
        return withStatus(OnboardingApplicationStatus.APPROVED, now, submittedAt, now);
    }

    public OnboardingApplication reject(Instant now) {
        assertTransition(OnboardingApplicationStatus.REJECTED);
        return withStatus(OnboardingApplicationStatus.REJECTED, now, submittedAt, now);
    }

    public OnboardingApplication failReview(Instant now) {
        assertTransition(OnboardingApplicationStatus.REVIEW_FAILED);
        return withStatus(OnboardingApplicationStatus.REVIEW_FAILED, now, submittedAt, decidedAt);
    }

    public OnboardingApplication retryReview(Instant now) {
        assertTransition(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW);
        return withStatus(OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW, now, submittedAt, decidedAt);
    }

    public OnboardingApplication startProvisioning(Instant now) {
        assertTransition(OnboardingApplicationStatus.PROVISIONING);
        return withStatus(OnboardingApplicationStatus.PROVISIONING, now, submittedAt, decidedAt);
    }

    public OnboardingApplication markProvisioningFailed(Instant now) {
        assertTransition(OnboardingApplicationStatus.PROVISIONING_FAILED);
        return withStatus(OnboardingApplicationStatus.PROVISIONING_FAILED, now, submittedAt, decidedAt);
    }

    public OnboardingApplication retryProvisioning(Instant now) {
        assertTransition(OnboardingApplicationStatus.PROVISIONING);
        return withStatus(OnboardingApplicationStatus.PROVISIONING, now, submittedAt, decidedAt);
    }

    public OnboardingApplication markCredentialSetupPending(Instant now) {
        assertTransition(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING);
        return withStatus(OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING, now, submittedAt, decidedAt);
    }

    public OnboardingApplication complete(Instant now) {
        assertTransition(OnboardingApplicationStatus.COMPLETED);
        return withStatus(OnboardingApplicationStatus.COMPLETED, now, submittedAt, decidedAt);
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
        return !EnumSet.of(OnboardingApplicationStatus.COMPLETED, OnboardingApplicationStatus.REJECTED,
                OnboardingApplicationStatus.EXPIRED, OnboardingApplicationStatus.CANCELLED).contains(status);
    }

    private OnboardingApplication withStatus(
            OnboardingApplicationStatus newStatus,
            Instant now,
            Instant newSubmittedAt,
            Instant newDecidedAt
    ) {
        return copy(newStatus, magicLinkTokenHash, magicLinkExpiresAt, magicLinkConsumedAt, emailVerifiedAt,
                continuationTokenHash, continuationExpiresAt, newSubmittedAt, newDecidedAt, now);
    }

    private OnboardingApplication copy(
            OnboardingApplicationStatus newStatus,
            String newMagicLinkTokenHash,
            Instant newMagicLinkExpiresAt,
            Instant newMagicLinkConsumedAt,
            Instant newEmailVerifiedAt,
            String newContinuationTokenHash,
            Instant newContinuationExpiresAt,
            Instant newSubmittedAt,
            Instant newDecidedAt,
            Instant newUpdatedAt
    ) {
        return new OnboardingApplication(
                id, email, newStatus, reviewMode, reviewPolicyVersion,
                newMagicLinkTokenHash, newMagicLinkExpiresAt, newMagicLinkConsumedAt, newEmailVerifiedAt,
                newContinuationTokenHash, newContinuationExpiresAt, expiresAt,
                newSubmittedAt, newDecidedAt, createdAt, newUpdatedAt, version
        );
    }

    private void assertTransition(OnboardingApplicationStatus requestedStatus) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(status, Set.of()).contains(requestedStatus)) {
            throw new InvalidOnboardingStatusTransitionException(status, requestedStatus);
        }
    }
}
