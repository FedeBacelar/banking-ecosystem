package com.fedebacelar.bank.gateway.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("banking.gateway.rate-limit.onboarding-start")
public record OnboardingStartRateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("3") int shortWindowRequests,
        @DefaultValue("PT1M") Duration shortWindow,
        @DefaultValue("10") int longWindowRequests,
        @DefaultValue("PT1H") Duration longWindow,
        @DefaultValue("30") int globalShortWindowRequests,
        @DefaultValue("300") int globalLongWindowRequests,
        @DefaultValue("10000") long maximumClients,
        @DefaultValue("PT2H") Duration clientIdleExpiration,
        @DefaultValue("64") int clientIpv6PrefixLength,
        String trustedProxies
) {

    private static final long MAXIMUM_TIMESTAMP_BUDGET = 1_000_000;

    public OnboardingStartRateLimitProperties {
        trustedProxies = trustedProxies == null ? "" : trustedProxies.trim();
        requireRange(shortWindowRequests, 1, 100, "short-window-requests");
        requireRange(longWindowRequests, shortWindowRequests, 10_000, "long-window-requests");
        requireDuration(shortWindow, Duration.ofSeconds(1), Duration.ofHours(1), "short-window");
        requireDuration(longWindow, shortWindow.plusNanos(1), Duration.ofDays(1), "long-window");
        requireRange(
                globalShortWindowRequests,
                shortWindowRequests,
                10_000,
                "global-short-window-requests"
        );
        requireRange(
                globalLongWindowRequests,
                Math.max(longWindowRequests, globalShortWindowRequests),
                100_000,
                "global-long-window-requests"
        );
        if (maximumClients < 1 || maximumClients > 100_000) {
            throw new IllegalArgumentException("maximum-clients must be between 1 and 100000");
        }
        if (maximumClients * longWindowRequests > MAXIMUM_TIMESTAMP_BUDGET) {
            throw new IllegalArgumentException("rate-limit client state exceeds the supported memory budget");
        }
        requireDuration(
                clientIdleExpiration,
                longWindow,
                Duration.ofDays(7),
                "client-idle-expiration"
        );
        requireRange(clientIpv6PrefixLength, 48, 128, "client-ipv6-prefix-length");
        if (trustedProxies.length() > 4_096) {
            throw new IllegalArgumentException("trusted-proxies is too long");
        }
    }

    private static void requireRange(int value, int minimum, int maximum, String property) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(property + " is outside the supported range");
        }
    }

    private static void requireDuration(
            Duration value,
            Duration minimum,
            Duration maximum,
            String property
    ) {
        if (value == null || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(property + " is outside the supported range");
        }
    }
}
