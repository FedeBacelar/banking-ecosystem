package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidateContinuationResponseTest {

    @Test
    void shouldMapApplicationIdToActiveContinuation() {
        UUID applicationId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-10T14:00:00Z");
        Instant updatedAt = Instant.parse("2026-07-10T12:00:00Z");
        ValidateContinuationResponse response = new ValidateContinuationResponse(
                applicationId,
                "applicant@example.com",
                OnboardingState.IN_PROGRESS,
                expiresAt,
                updatedAt
        );

        var continuation = response.toSession();

        assertThat(continuation.active()).isTrue();
        assertThat(continuation.applicationId()).isEqualTo(applicationId);
        assertThat(continuation.status()).isEqualTo(OnboardingState.IN_PROGRESS);
        assertThat(continuation.updatedAt()).isEqualTo(updatedAt);
    }
}
