package com.fedebacelar.bank.homebanking.bff.domain.model;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

public record HomeBankingContext(
        String subject,
        String username,
        String email,
        UUID customerId,
        JsonNode customer,
        List<JsonNode> accounts
) {
}
