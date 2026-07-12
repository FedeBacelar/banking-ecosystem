package com.fedebacelar.bank.onboarding.domain.exception;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;

public class InvalidOnboardingStatusTransitionException extends RuntimeException {

    public InvalidOnboardingStatusTransitionException(
            OnboardingApplicationStatus currentStatus,
            OnboardingApplicationStatus requestedStatus
    ) {
        super("Cannot move onboarding application from " + currentStatus + " to " + requestedStatus);
    }
}
