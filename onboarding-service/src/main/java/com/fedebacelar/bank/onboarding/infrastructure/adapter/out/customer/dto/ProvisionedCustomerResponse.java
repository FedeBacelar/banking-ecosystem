package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.customer.dto;

import java.util.UUID;

public record ProvisionedCustomerResponse(UUID customerId, String status) {
}
