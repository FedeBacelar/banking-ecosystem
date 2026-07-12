package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OnboardingSubmission(UUID applicationId, OnboardingState status, Instant submittedAt, Instant updatedAt) {
}
