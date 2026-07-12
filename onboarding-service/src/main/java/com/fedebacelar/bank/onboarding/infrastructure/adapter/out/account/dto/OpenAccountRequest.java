package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.account.dto;

import java.util.UUID;

public record OpenAccountRequest(UUID customerId, String type, String currency, String alias) {
}
