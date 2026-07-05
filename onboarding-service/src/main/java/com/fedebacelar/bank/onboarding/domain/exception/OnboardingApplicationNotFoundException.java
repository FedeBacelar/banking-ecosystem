package com.fedebacelar.bank.onboarding.domain.exception;

import java.util.UUID;

public class OnboardingApplicationNotFoundException extends RuntimeException {

    public OnboardingApplicationNotFoundException(UUID applicationId) {
        super("Onboarding application not found: " + applicationId);
    }
}
