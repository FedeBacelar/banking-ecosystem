package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingApplication(
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
}
