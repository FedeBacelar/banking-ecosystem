package com.fedebacelar.bank.onboarding.domain.exception;

public class TermsAcceptanceRequiredException extends RuntimeException {

    public TermsAcceptanceRequiredException() {
        super("Terms acceptance is required to continue onboarding.");
    }
}
