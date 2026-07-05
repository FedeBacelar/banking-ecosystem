package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

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

    public static OnboardingApplicationResponse from(OnboardingApplication application) {
        return new OnboardingApplicationResponse(
                application.id(),
                application.email(),
                application.status(),
                application.magicLinkExpiresAt(),
                application.emailVerifiedAt(),
                application.continuationExpiresAt(),
                application.expiresAt(),
                application.createdAt(),
                application.updatedAt()
        );
    }
}
