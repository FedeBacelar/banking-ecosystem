package com.fedebacelar.bank.onboarding.domain.exception;

public class InvalidContinuationTokenException extends RuntimeException {

    public InvalidContinuationTokenException() {
        super("Invalid onboarding continuation token");
    }
}
