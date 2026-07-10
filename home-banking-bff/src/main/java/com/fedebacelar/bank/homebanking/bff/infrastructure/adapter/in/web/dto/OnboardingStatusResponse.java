package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingPublicStatus;
import java.time.Instant;
import java.util.UUID;

public record OnboardingStatusResponse(UUID applicationId, String status, String nextAction, Instant updatedAt) {
    public static OnboardingStatusResponse from(OnboardingPublicStatus status) {
        return new OnboardingStatusResponse(status.applicationId(), status.status(), status.nextAction(), status.updatedAt());
    }
}
