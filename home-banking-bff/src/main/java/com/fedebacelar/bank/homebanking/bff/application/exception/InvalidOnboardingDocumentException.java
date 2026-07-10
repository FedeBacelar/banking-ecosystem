package com.fedebacelar.bank.homebanking.bff.application.exception;

public class InvalidOnboardingDocumentException extends RuntimeException {

    public InvalidOnboardingDocumentException(String message) {
        super(message);
    }
}
