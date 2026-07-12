package com.fedebacelar.bank.onboarding.domain.exception;

public class OnboardingDocumentUploadException extends RuntimeException {
    public OnboardingDocumentUploadException(String message) {
        super(message);
    }

    public OnboardingDocumentUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
