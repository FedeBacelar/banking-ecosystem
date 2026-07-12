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
    CREDENTIAL_SETUP_EXPIRED,
    CREDENTIAL_SETUP_FAILED,
    COMPLETED,
    REJECTED,
    EXPIRED,
    CANCELLED,
    PROVISIONING_FAILED;

    public boolean allowsApplicantAccessRecovery() {
        return switch (this) {
            case COMPLETED, REJECTED, EXPIRED, CANCELLED,
                    CREDENTIAL_SETUP_EXPIRED, CREDENTIAL_SETUP_FAILED -> false;
            default -> true;
        };
    }
}
