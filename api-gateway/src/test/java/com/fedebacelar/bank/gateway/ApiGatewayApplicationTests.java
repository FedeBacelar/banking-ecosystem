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

@SpringBootTest(properties = "otel.sdk.disabled=false")
class ApiGatewayApplicationTests {

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldKeepTelemetryDisabledByDefault() {
        assertThat(openTelemetry).isSameAs(OpenTelemetry.noop());
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(W3cTracePropagationFilter.class)).isEmpty();
        Span span = openTelemetry.getTracer("nerva-test").spanBuilder("disabled-by-default").startSpan();
        try {
            assertThat(span.getSpanContext().isValid()).isFalse();
        } finally {
            span.end();
        }
    }

}
