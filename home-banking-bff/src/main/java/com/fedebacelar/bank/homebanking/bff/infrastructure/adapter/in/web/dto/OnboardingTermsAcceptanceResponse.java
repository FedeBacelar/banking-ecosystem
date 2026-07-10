package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import java.time.Instant;
import java.util.UUID;

public record OnboardingTermsAcceptanceResponse(
        UUID applicationId,
        String termsVersion,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static OnboardingTermsAcceptanceResponse from(OnboardingTermsAcceptance acceptance) {
        return new OnboardingTermsAcceptanceResponse(
                acceptance.applicationId(),
                acceptance.termsVersion(),
                acceptance.acceptedAt(),
                acceptance.createdAt(),
                acceptance.updatedAt()
        );
    }
}
