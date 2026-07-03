package com.fedebacelar.bank.homebanking.bff.application.exception;

public class IdentityNotLinkedException extends RuntimeException {

    private final String providerSubject;

    public IdentityNotLinkedException(String providerSubject) {
        super("Authenticated identity is not linked to a banking customer");
        this.providerSubject = providerSubject;
    }

    public String providerSubject() {
        return providerSubject;
    }
}
