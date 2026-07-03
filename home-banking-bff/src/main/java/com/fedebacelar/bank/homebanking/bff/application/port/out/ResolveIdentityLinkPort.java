package com.fedebacelar.bank.homebanking.bff.application.port.out;

import com.fedebacelar.bank.homebanking.bff.domain.model.IdentityLink;

public interface ResolveIdentityLinkPort {

    IdentityLink resolveByKeycloakSubject(String providerSubject, String accessToken);
}
