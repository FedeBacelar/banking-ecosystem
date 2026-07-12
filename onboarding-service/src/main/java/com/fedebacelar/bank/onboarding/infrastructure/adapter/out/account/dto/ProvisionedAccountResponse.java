package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto;

import java.util.UUID;

public record ProvisionedAccountResponse(UUID accountId, UUID customerId, String status) {
}
