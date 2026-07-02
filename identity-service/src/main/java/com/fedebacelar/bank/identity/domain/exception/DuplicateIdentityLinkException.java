package com.fedebacelar.bank.identity.domain.exception;

import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;

public class DuplicateIdentityLinkException extends RuntimeException {

    public DuplicateIdentityLinkException(IdentityProvider provider, String providerSubject) {
        super("Identity link already exists for provider " + provider + " and subject " + providerSubject);
    }
}
