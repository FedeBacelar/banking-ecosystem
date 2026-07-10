package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import java.time.Instant;
import java.util.UUID;

public record TermsAcceptanceResponse(
        UUID applicationId,
        String termsVersion,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public OnboardingTermsAcceptance toDomain() {
        return new OnboardingTermsAcceptance(
                applicationId,
                termsVersion,
                acceptedAt,
                createdAt,
                updatedAt
        );
    }
}
