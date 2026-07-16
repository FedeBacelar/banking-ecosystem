package com.fedebacelar.bank.homebanking.bff.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("!observability")
public class DisabledOpenTelemetryConfiguration {

    @Bean
    OpenTelemetry openTelemetry() {
        return OpenTelemetry.noop();
    }
}
