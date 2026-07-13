package com.fedebacelar.bank.onboarding.domain.exception;

public class InvalidOnboardingDocumentException extends RuntimeException {

    public InvalidOnboardingDocumentException(Throwable cause) {
        super("The onboarding document is invalid.", cause);
    }
}
