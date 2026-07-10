package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

public record AcceptTermsRequest(
        String continuationToken,
        boolean accepted,
        String termsVersion
) {
}
