package com.fedebacelar.bank.identity;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IdentityServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Test
    void contextLoadsWithObservabilityDisabledByDefault() {
        assertThat(openTelemetry).isSameAs(OpenTelemetry.noop());
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).isEmpty();
    }
}
