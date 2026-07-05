package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record OnboardingApplicationResponse(
        UUID id,
        String email,
        OnboardingApplicationStatus status,
        Instant magicLinkExpiresAt,
        Instant magicLinkConsumedAt,
        Instant emailVerifiedAt,
        Instant continuationExpiresAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
