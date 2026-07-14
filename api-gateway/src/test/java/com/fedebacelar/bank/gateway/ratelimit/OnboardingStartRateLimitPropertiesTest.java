package com.fedebacelar.bank.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OnboardingStartRateLimitPropertiesTest {

    @Test
    void shouldRejectALongWindowThatDoesNotContainTheShortWindow() {
        assertThatThrownBy(() -> properties(
                3,
                Duration.ofMinutes(1),
                10,
                Duration.ofSeconds(30),
                Duration.ofHours(2)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("long-window");
    }

    @Test
    void shouldRejectAnIdleExpirationShorterThanTheLongWindow() {
        assertThatThrownBy(() -> properties(
                3,
                Duration.ofMinutes(1),
                10,
                Duration.ofHours(1),
                Duration.ofMinutes(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client-idle-expiration");
    }

    @Test
    void shouldRejectACapacityThatWouldMakeTheShortWindowStricterThanConfigured() {
        assertThatThrownBy(() -> properties(
                5,
                Duration.ofMinutes(1),
                4,
                Duration.ofHours(1),
                Duration.ofHours(2)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("long-window-requests");
    }

    @Test
    void shouldRejectAClientStateConfigurationThatExceedsTheMemoryBudget() {
        assertThatThrownBy(() -> new OnboardingStartRateLimitProperties(
                true,
                3,
                Duration.ofMinutes(1),
                100,
                Duration.ofHours(1),
                100,
                1_000,
                100_000,
                Duration.ofHours(2),
                64,
                ""
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memory budget");
    }

    @Test
    void shouldRejectAnIpv6ClientPrefixThatIsTooBroad() {
        assertThatThrownBy(() -> new OnboardingStartRateLimitProperties(
                true,
                3,
                Duration.ofMinutes(1),
                10,
                Duration.ofHours(1),
                30,
                300,
                10_000,
                Duration.ofHours(2),
                32,
                ""
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client-ipv6-prefix-length");
    }

    private OnboardingStartRateLimitProperties properties(
            int shortRequests,
            Duration shortWindow,
            int longRequests,
            Duration longWindow,
            Duration idleExpiration
    ) {
        return new OnboardingStartRateLimitProperties(
                true,
                shortRequests,
                shortWindow,
                longRequests,
                longWindow,
                30,
                300,
                50_000,
                idleExpiration,
                64,
                ""
        );
    }
}
