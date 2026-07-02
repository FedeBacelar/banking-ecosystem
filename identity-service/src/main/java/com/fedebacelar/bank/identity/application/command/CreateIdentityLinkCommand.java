package com.fedebacelar.bank.identity.application.command;

import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import java.util.UUID;

public record CreateIdentityLinkCommand(
        UUID customerId,
        IdentityProvider provider,
        String providerSubject
) {
}
