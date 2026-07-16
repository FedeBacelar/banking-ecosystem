package com.fedebacelar.bank.notification.infrastructure.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.infrastructure.config.DisabledOpenTelemetryConfiguration;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class NotificationTelemetryProfileTest {

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
                    NoOpNotificationTelemetryAdapter.class,
                    MicrometerNotificationTelemetryAdapter.class
            );

    @Test
    void remainsDisabledWithoutObservabilityProfileEvenWhenEnvironmentRequestsTheSdk() {
        contextRunner.run(context -> {
            NotificationTelemetryPort telemetry = context.getBean(NotificationTelemetryPort.class);
            SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

            assertThat(telemetry).isInstanceOf(NoOpNotificationTelemetryAdapter.class);
            assertThat(context.getBean(OpenTelemetry.class))
                    .isSameAs(OpenTelemetry.noop())
                    .isNotInstanceOf(OpenTelemetrySdk.class);

            telemetry.recordDelivery(
                    NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                    NotificationTelemetryPort.DeliveryOutcome.SENT
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
                    NotificationTelemetryPort telemetry = context.getBean(NotificationTelemetryPort.class);
                    SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

                    assertThat(telemetry).isInstanceOf(MicrometerNotificationTelemetryAdapter.class);

                    telemetry.recordDelivery(
                            NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                            NotificationTelemetryPort.DeliveryOutcome.SENT
                    );

                    assertThat(registry.get("nerva.notification.deliveries")
                            .tag("template", "onboarding_email_magic_link")
                            .tag("outcome", "sent")
                            .counter().count()).isEqualTo(1.0);
                });
    }
}
