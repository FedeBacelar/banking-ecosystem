package com.fedebacelar.bank.identity.application.port.in;

import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;

public interface ResolveIdentityLinkUseCase {

    IdentityLinkDetails resolveActive(IdentityProvider provider, String providerSubject);
}
