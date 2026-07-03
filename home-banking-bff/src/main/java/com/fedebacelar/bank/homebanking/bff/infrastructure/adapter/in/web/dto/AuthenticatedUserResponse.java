package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUserResponse(
        String subject,
        String username,
        String email,
        UUID customerId,
        JsonNode customer,
        List<JsonNode> accounts
) {
}
