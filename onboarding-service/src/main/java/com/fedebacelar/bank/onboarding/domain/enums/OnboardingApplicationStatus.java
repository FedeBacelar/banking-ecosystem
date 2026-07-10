package com.fedebacelar.bank.onboarding.domain.enums;

public enum OnboardingApplicationStatus {
    EMAIL_VERIFICATION_PENDING,
    IN_PROGRESS,
    SUBMITTED,
    UNDER_AUTOMATED_REVIEW,
    REVIEW_FAILED,
    APPROVED,
    PROVISIONING,
    CREDENTIAL_SETUP_PENDING,
    COMPLETED,
    REJECTED,
    EXPIRED,
    CANCELLED,
    PROVISIONING_FAILED
}
