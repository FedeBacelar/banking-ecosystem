package com.fedebacelar.bank.customer.application.command;

import java.util.UUID;

public record CustomerReasonCommand(
        UUID customerId,
        String reason
) {
}
