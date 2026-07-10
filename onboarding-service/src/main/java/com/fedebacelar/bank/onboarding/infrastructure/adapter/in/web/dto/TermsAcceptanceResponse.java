package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record TermsAcceptanceResponse(
        UUID applicationId,
        String termsVersion,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
