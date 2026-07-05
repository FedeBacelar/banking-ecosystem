package com.fedebacelar.bank.onboarding.domain.exception;

public class DuplicateActiveOnboardingApplicationException extends RuntimeException {

    public DuplicateActiveOnboardingApplicationException(String email) {
        super("An active onboarding application already exists for email: " + email);
    }
}
