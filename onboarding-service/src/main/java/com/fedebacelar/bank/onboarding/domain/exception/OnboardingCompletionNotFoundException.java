package com.fedebacelar.bank.onboarding.domain.exception;

public class OnboardingCompletionNotFoundException extends RuntimeException {

    public OnboardingCompletionNotFoundException() {
        super("No onboarding completion process was found for the authenticated identity.");
    }
}
