package com.fedebacelar.bank.identity.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import java.time.Instant;
import java.util.UUID;

public record IdentityLinkResponse(
        UUID id,
        UUID customerId,
        IdentityProvider provider,
        String providerSubject,
        IdentityLinkStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
