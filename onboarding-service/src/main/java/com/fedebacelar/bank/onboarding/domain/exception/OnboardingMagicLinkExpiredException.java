package com.fedebacelar.bank.onboarding.domain.exception;

public class OnboardingMagicLinkExpiredException extends RuntimeException {

    public OnboardingMagicLinkExpiredException() {
        super("Onboarding magic link token expired");
    }
}
