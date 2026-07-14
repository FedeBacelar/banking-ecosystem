package com.fedebacelar.bank.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OnboardingStartRateLimitProperties.class)
class RateLimitConfiguration {

    @Bean
    TrustedClientIpResolver trustedClientIpResolver(OnboardingStartRateLimitProperties properties) {
        return new TrustedClientIpResolver(
                properties.trustedProxies(),
                properties.clientIpv6PrefixLength()
        );
    }

    @Bean
    InitializingBean forwardedHeaderStrategyGuard(Environment environment) {
        return () -> {
            String strategy = environment.getProperty("server.forward-headers-strategy", "none");
            if (!"none".equalsIgnoreCase(strategy.trim())) {
                throw new IllegalStateException(
                        "The gateway rate limiter requires server.forward-headers-strategy=none"
                );
            }
        };
    }

    @Bean
    LocalOnboardingStartRateLimiter localOnboardingStartRateLimiter(
            OnboardingStartRateLimitProperties properties
    ) {
        return new LocalOnboardingStartRateLimiter(properties, Ticker.systemTicker());
    }
}
