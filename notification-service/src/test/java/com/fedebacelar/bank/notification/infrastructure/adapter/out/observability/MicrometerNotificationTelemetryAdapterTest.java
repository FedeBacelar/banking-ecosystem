package com.fedebacelar.bank.notification.infrastructure.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MicrometerNotificationTelemetryAdapterTest {

    @Test
    void recordsOnlyBoundedTemplateAndOutcomeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var telemetry = new MicrometerNotificationTelemetryAdapter(registry);

        telemetry.recordDelivery(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                NotificationTelemetryPort.DeliveryOutcome.SENT
        );

        assertThat(registry.get("nerva.notification.deliveries")
                .tag("template", "onboarding_email_magic_link")
                .tag("outcome", "sent")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.find("nerva.notification.deliveries").meters())
                .hasSize(NotificationTemplateCode.values().length
                        * NotificationTelemetryPort.DeliveryOutcome.values().length);
    }
}
