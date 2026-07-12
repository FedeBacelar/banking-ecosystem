package com.fedebacelar.bank.onboarding.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.exception.ProvisioningRequestMismatchException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingProvisioningStepTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void permitsRetriesOnlyWhenTheCanonicalRequestIsUnchanged() {
        OnboardingProvisioningStep running = OnboardingProvisioningStep.pending(
                UUID.randomUUID(), ProvisioningStepType.CREATE_CUSTOMER, NOW.minusSeconds(10)
        ).start("hash-a", NOW.minusSeconds(5));

        assertThat(running.retry("TEMPORARY", NOW.plusSeconds(5), NOW).start("hash-a", NOW.plusSeconds(5)).attempts())
                .isEqualTo(2);
        assertThatThrownBy(() -> running.retry("TEMPORARY", NOW.plusSeconds(5), NOW)
                .start("hash-b", NOW.plusSeconds(5)))
                .isInstanceOf(ProvisioningRequestMismatchException.class);
    }
}
