package com.fedebacelar.bank.homebanking.bff.application.exception;

public class OnboardingSessionRequiredException extends RuntimeException {

    public OnboardingSessionRequiredException() {
        super("Onboarding continuation session is required");
    }
}
