package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import java.time.Instant;
import java.util.UUID;

public record OnboardingSessionResponse(
        boolean active,
        UUID applicationId,
        String status,
        Instant continuationExpiresAt
) {

    public static OnboardingSessionResponse from(OnboardingSession session) {
        return new OnboardingSessionResponse(
                session.active(),
                session.applicationId(),
                session.status(),
                session.continuationExpiresAt()
        );
    }
}
