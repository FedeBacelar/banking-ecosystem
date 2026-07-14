package com.fedebacelar.bank.homebanking.bff.application.exception;

public class InternalAccessUnavailableException extends RuntimeException {

    public InternalAccessUnavailableException() {
        super("Internal service access is unavailable.");
    }

    public InternalAccessUnavailableException(Throwable cause) {
        super("Internal service access is unavailable.", cause);
    }
}
