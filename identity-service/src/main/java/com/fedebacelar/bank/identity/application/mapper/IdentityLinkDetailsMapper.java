package com.fedebacelar.bank.identity.application.mapper;

import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;
import com.fedebacelar.bank.identity.domain.model.IdentityLink;

public final class IdentityLinkDetailsMapper {

    private IdentityLinkDetailsMapper() {
    }

    public static IdentityLinkDetails toDetails(IdentityLink identityLink) {
        return new IdentityLinkDetails(
                identityLink.id(),
                identityLink.customerId(),
                identityLink.provider(),
                identityLink.providerSubject(),
                identityLink.status(),
                identityLink.createdAt(),
                identityLink.updatedAt(),
                identityLink.version()
        );
    }
}
