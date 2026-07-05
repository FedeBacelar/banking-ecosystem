package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import java.time.Instant;
import java.util.UUID;

public record OnboardingApplicationResponse(
        UUID id,
        String email,
        String status,
        Instant magicLinkExpiresAt,
        Instant emailVerifiedAt,
        Instant continuationExpiresAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {

    public OnboardingApplication toDomain() {
        return new OnboardingApplication(
                id,
                email,
                status,
                magicLinkExpiresAt,
                emailVerifiedAt,
                continuationExpiresAt,
                expiresAt,
                createdAt,
                updatedAt
        );
    }
}
