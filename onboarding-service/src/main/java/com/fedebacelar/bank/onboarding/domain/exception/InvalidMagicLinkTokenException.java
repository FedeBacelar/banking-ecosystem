package com.fedebacelar.bank.onboarding.domain.exception;

public class InvalidMagicLinkTokenException extends RuntimeException {

    public InvalidMagicLinkTokenException() {
        super("Invalid onboarding magic link token");
    }
}
