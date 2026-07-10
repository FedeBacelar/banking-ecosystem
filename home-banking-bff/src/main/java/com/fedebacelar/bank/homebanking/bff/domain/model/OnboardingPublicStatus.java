package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingPublicStatus(UUID applicationId, String status, String nextAction, Instant updatedAt) {
}
