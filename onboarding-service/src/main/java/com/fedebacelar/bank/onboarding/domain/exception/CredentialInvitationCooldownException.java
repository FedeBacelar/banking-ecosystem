package com.fedebacelar.bank.onboarding.domain.exception;

public class CredentialInvitationCooldownException extends RuntimeException {
    public CredentialInvitationCooldownException() {
        super("Credential invitation was sent recently.");
    }
}
