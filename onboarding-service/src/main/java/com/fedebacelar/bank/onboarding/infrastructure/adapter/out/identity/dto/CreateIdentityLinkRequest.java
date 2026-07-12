package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity.dto;

import java.util.UUID;

public record CreateIdentityLinkRequest(UUID customerId, String provider, String providerSubject) {
}
