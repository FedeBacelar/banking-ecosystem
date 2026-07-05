package com.fedebacelar.bank.onboarding.domain.exception;

public class OnboardingMagicLinkAlreadyConsumedException extends RuntimeException {

    public OnboardingMagicLinkAlreadyConsumedException() {
        super("Onboarding magic link token was already consumed");
    }
}
