package com.fedebacelar.bank.identity.domain.exception;

import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;

public class InactiveIdentityLinkException extends RuntimeException {

    public InactiveIdentityLinkException(IdentityProvider provider, String providerSubject, IdentityLinkStatus status) {
        super("Identity link for provider " + provider + " and subject " + providerSubject + " is not active. Current status: " + status);
    }
}
