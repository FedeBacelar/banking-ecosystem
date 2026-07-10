package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity.dto;

import java.util.UUID;

public record ProvisionedIdentityResponse(UUID id, UUID customerId, String provider, String providerSubject, String status) {
}
