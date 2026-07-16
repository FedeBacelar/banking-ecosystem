package com.fedebacelar.bank.account.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.account.TestcontainersConfiguration;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "otel.traces.exporter=none",
        "otel.metrics.exporter=none",
        "otel.logs.exporter=none"
})
@ActiveProfiles("observability")
@Import(TestcontainersConfiguration.class)
class ObservabilityProfileTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Test
    void enablesOpenTelemetryPrometheusAndFeignPropagationWithTheProfile() {
        assertThat(openTelemetry).isNotSameAs(OpenTelemetry.noop());
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(W3cFeignTracePropagationInterceptor.class)).hasSize(1);
    }
}
