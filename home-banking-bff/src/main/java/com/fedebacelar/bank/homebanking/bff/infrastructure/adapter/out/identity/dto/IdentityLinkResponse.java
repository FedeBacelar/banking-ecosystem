package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.identity.dto;

import java.util.UUID;

public record IdentityLinkResponse(
        UUID id,
        UUID customerId,
        String provider,
        String providerSubject,
        String status
) {
}
