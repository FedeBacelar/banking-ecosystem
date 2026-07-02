package com.fedebacelar.bank.identity.domain.exception;

import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import java.util.UUID;

public class IdentityLinkNotFoundException extends RuntimeException {

    public IdentityLinkNotFoundException(UUID identityLinkId) {
        super("Identity link not found: " + identityLinkId);
    }

    public IdentityLinkNotFoundException(IdentityProvider provider, String providerSubject) {
        super("Identity link not found for provider " + provider + " and subject " + providerSubject);
    }
}
