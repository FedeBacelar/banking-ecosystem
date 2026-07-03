package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

public record SessionResponse(
        boolean authenticated,
        String subject,
        String username
) {
}
