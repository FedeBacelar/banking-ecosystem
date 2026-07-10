package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingApplicationResponseTest {

    @Test
    void shouldMapApplicationResponseToDomain() {
        UUID applicationId = UUID.randomUUID();
        OnboardingApplicationResponse response = new OnboardingApplicationResponse(
                applicationId,
                "applicant@example.com",
                "IN_PROGRESS",
                Instant.parse("2026-07-05T10:30:00Z"),
                Instant.parse("2026-07-05T10:10:00Z"),
                Instant.parse("2026-07-05T12:00:00Z"),
                Instant.parse("2026-07-06T10:00:00Z"),
                Instant.parse("2026-07-05T10:00:00Z"),
                Instant.parse("2026-07-05T10:10:00Z")
        );

        assertThat(response.toDomain().id()).isEqualTo(applicationId);
        assertThat(response.toDomain().email()).isEqualTo("applicant@example.com");
    }
}
