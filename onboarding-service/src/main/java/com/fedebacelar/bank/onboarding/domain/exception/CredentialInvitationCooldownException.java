package com.fedebacelar.bank.onboarding.domain.exception;

public class CredentialInvitationCooldownException extends RuntimeException {
    private final long retryAfterSeconds;

    public CredentialInvitationCooldownException(long retryAfterSeconds) {
        super("Credential invitation was sent recently.");
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
