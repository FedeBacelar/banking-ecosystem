package com.fedebacelar.bank.onboarding.domain.exception;

public class OnboardingContinuationExpiredException extends RuntimeException {

    public OnboardingContinuationExpiredException() {
        super("Onboarding continuation token expired");
    }
}
