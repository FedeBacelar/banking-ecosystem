package com.fedebacelar.bank.homebanking.bff.domain.model;

public record AuthenticatedUser(
        String subject,
        String username,
        String email
) {
}
