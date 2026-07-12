package com.fedebacelar.bank.homebanking.bff.domain.model;

public enum OnboardingState {
    EMAIL_VERIFICATION_PENDING,
    IN_PROGRESS,
    SUBMITTED,
    UNDER_AUTOMATED_REVIEW,
    REVIEW_FAILED,
    APPROVED,
    PROVISIONING,
    PROVISIONING_FAILED,
    CREDENTIAL_SETUP_PENDING,
    CREDENTIAL_SETUP_EXPIRED,
    CREDENTIAL_SETUP_FAILED,
    COMPLETED,
    REJECTED,
    EXPIRED,
    CANCELLED;

    public OnboardingNextAction nextAction() {
        return switch (this) {
            case IN_PROGRESS -> OnboardingNextAction.CONTINUE_APPLICATION;
            case EMAIL_VERIFICATION_PENDING, CREDENTIAL_SETUP_PENDING -> OnboardingNextAction.CHECK_EMAIL;
            case SUBMITTED, UNDER_AUTOMATED_REVIEW, APPROVED, PROVISIONING -> OnboardingNextAction.WAIT;
            case COMPLETED -> OnboardingNextAction.LOGIN;
            case REJECTED, REVIEW_FAILED, PROVISIONING_FAILED,
                    CREDENTIAL_SETUP_EXPIRED, CREDENTIAL_SETUP_FAILED -> OnboardingNextAction.CONTACT_SUPPORT;
            case EXPIRED, CANCELLED -> OnboardingNextAction.START_NEW_APPLICATION;
        };
    }
}
