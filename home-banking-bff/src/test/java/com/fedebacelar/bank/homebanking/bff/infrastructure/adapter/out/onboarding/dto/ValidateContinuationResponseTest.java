package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidateContinuationResponseTest {

    @Test
    void shouldMapApplicationIdToActiveSession() {
        UUID applicationId = UUID.randomUUID();
        Instant continuationExpiresAt = Instant.parse("2026-07-05T12:00:00Z");
        ValidateContinuationResponse response = new ValidateContinuationResponse(
                applicationId,
                "applicant@example.com",
                "IN_PROGRESS",
                continuationExpiresAt
        );

        OnboardingSession session = response.toSession();

        assertThat(session.active()).isTrue();
        assertThat(session.applicationId()).isEqualTo(applicationId);
        assertThat(session.status()).isEqualTo("IN_PROGRESS");
        assertThat(session.continuationExpiresAt()).isEqualTo(continuationExpiresAt);
    }
}
