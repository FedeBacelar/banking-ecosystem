package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingPublicStatus;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingNextAction;
import java.time.Instant;
import java.util.UUID;

public record OnboardingStatusResponse(
        UUID applicationId,
        OnboardingState status,
        OnboardingNextAction nextAction,
        Instant updatedAt
) {
    public static OnboardingStatusResponse from(OnboardingPublicStatus status) {
        return new OnboardingStatusResponse(status.applicationId(), status.status(), status.nextAction(), status.updatedAt());
    }
}
