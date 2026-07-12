package com.fedebacelar.bank.onboarding.domain.exception;

public class CredentialIdentityConflictException extends RuntimeException {
    public CredentialIdentityConflictException() {
        super("Email is already associated with another credential identity.");
    }
}
