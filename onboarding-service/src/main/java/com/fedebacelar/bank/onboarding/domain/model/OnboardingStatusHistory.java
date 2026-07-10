package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record OnboardingStatusHistory(
        UUID id,
        UUID applicationId,
        OnboardingApplicationStatus previousStatus,
        OnboardingApplicationStatus newStatus,
        String reasonCode,
        OnboardingActorType actorType,
        Instant occurredAt
) {
    public static OnboardingStatusHistory transition(
            UUID applicationId,
            OnboardingApplicationStatus previousStatus,
            OnboardingApplicationStatus newStatus,
            String reasonCode,
            OnboardingActorType actorType,
            Instant now
    ) {
        return new OnboardingStatusHistory(UUID.randomUUID(), applicationId, previousStatus, newStatus, reasonCode, actorType, now);
    }
}
