package com.fedebacelar.bank.onboarding.domain.exception;

public class OnboardingDocumentTooLargeException extends RuntimeException {

    public OnboardingDocumentTooLargeException(Throwable cause) {
        super("The onboarding document is too large.", cause);
    }
}
