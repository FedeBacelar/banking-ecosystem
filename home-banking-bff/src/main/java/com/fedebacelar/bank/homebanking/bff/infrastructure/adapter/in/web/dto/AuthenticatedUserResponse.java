package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

public record AuthenticatedUserResponse(
        String username,
        String displayName
) {
}
