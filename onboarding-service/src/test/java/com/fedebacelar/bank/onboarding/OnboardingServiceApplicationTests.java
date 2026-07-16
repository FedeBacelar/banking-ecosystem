package com.fedebacelar.bank.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability.NoOpOnboardingTelemetryAdapter;
import com.fedebacelar.bank.onboarding.infrastructure.config.W3cFeignTracePropagationInterceptor;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OnboardingServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OnboardingTelemetryPort telemetry;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Test
    void contextLoadsWithObservabilityDisabledByDefault() {
        assertThat(telemetry).isInstanceOf(NoOpOnboardingTelemetryAdapter.class);
        assertThat(openTelemetry).isSameAs(OpenTelemetry.noop());
        assertThat(applicationContext.getBeansOfType(W3cFeignTracePropagationInterceptor.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).isEmpty();
    }
}
