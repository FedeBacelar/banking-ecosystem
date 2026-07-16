package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.infrastructure.config.DisabledOpenTelemetryConfiguration;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class OnboardingTelemetryProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                    new SystemEnvironmentPropertySource(
                            "testEnvironment",
                            Map.of("OTEL_SDK_DISABLED", "false")
                    )
            ))
            .withBean(SimpleMeterRegistry.class)
            .withUserConfiguration(
                    DisabledOpenTelemetryConfiguration.class,
                    NoOpOnboardingTelemetryAdapter.class,
                    MicrometerOnboardingTelemetryAdapter.class
            );

    @Test
    void remainsDisabledWithoutObservabilityProfileEvenWhenEnvironmentRequestsTheSdk() {
        contextRunner.run(context -> {
            OnboardingTelemetryPort telemetry = context.getBean(OnboardingTelemetryPort.class);
            SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

            assertThat(telemetry).isInstanceOf(NoOpOnboardingTelemetryAdapter.class);
            assertThat(context.getBean(OpenTelemetry.class))
                    .isSameAs(OpenTelemetry.noop())
                    .isNotInstanceOf(OpenTelemetrySdk.class);

            telemetry.recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.CREATED);
            telemetry.recordWorkOutcome(
                    OnboardingTelemetryPort.WorkType.AUTO_REVIEW,
                    OnboardingTelemetryPort.WorkOutcome.SUCCEEDED
            );

            assertThat(registry.getMeters()).isEmpty();
        });
    }

    @Test
    void enablesFunctionalTelemetryOnlyWithObservabilityProfile() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("observability"))
                .withBean(OpenTelemetry.class, OpenTelemetry::noop)
                .run(context -> {
                    OnboardingTelemetryPort telemetry = context.getBean(OnboardingTelemetryPort.class);
                    SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

                    assertThat(telemetry).isInstanceOf(MicrometerOnboardingTelemetryAdapter.class);

                    telemetry.recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.CREATED);

                    assertThat(registry.get("nerva.onboarding.applications")
                            .tag("event", "created")
                            .counter().count()).isEqualTo(1.0);
                });
    }
}
