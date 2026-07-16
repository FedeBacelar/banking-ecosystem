package com.fedebacelar.bank.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.gateway.observability.W3cTracePropagationFilter;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "otel.traces.exporter=none",
        "otel.metrics.exporter=none",
        "otel.logs.exporter=none"
})
@ActiveProfiles("observability")
class ObservabilityProfileTest {

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldEnableTelemetryWithTheObservabilityProfile() {
        assertThat(environment.getProperty("management.prometheus.metrics.export.enabled", Boolean.class))
                .isTrue();
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(W3cTracePropagationFilter.class)).hasSize(1);
        assertThat(applicationContext.containsBean("gatewayRouteClientObservationMeterFilter")).isTrue();
        Span span = openTelemetry.getTracer("nerva-test").spanBuilder("enabled-profile").startSpan();
        try {
            assertThat(span.getSpanContext().isValid()).isTrue();
        } finally {
            span.end();
        }
    }
}
