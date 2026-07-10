package com.fedebacelar.bank.customer.application.command;

import java.util.UUID;

public record ApproveCustomerKycCommand(UUID customerId, String reasonCode, String changedBy) {
}
