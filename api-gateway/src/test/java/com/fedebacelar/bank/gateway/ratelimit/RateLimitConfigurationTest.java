package com.fedebacelar.bank.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RateLimitConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RateLimitConfiguration.class);

    @Test
    void shouldStartWithTheSafeForwardedHeaderStrategy() {
        contextRunner
                .withPropertyValues("server.forward-headers-strategy=none")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OnboardingStartRateLimitProperties.class);
                    assertThat(context).hasSingleBean(TrustedClientIpResolver.class);
                    assertThat(context).hasSingleBean(LocalOnboardingStartRateLimiter.class);
                });
    }

    @Test
    void shouldFailBeforeServingTrafficWhenTheServerReinterpretsForwardedHeaders() {
        contextRunner
                .withPropertyValues("server.forward-headers-strategy=native")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "The gateway rate limiter requires server.forward-headers-strategy=none"
                            );
                });
    }
}
