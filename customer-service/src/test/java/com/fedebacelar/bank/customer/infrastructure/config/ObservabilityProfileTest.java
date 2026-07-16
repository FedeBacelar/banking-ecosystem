package com.fedebacelar.bank.customer.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "otel.traces.exporter=none",
        "otel.metrics.exporter=none",
        "otel.logs.exporter=none"
})
@ActiveProfiles("observability")
@Testcontainers(disabledWithoutDocker = true)
class ObservabilityProfileTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Test
    void enablesOpenTelemetryAndPrometheusOnlyWithTheProfile() {
        assertThat(openTelemetry).isNotSameAs(OpenTelemetry.noop());
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).hasSize(1);
    }
}
