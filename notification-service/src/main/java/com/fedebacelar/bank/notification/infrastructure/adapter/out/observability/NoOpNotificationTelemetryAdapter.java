package com.fedebacelar.bank.notification.infrastructure.adapter.out.observability;

import com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!observability")
public class NoOpNotificationTelemetryAdapter implements NotificationTelemetryPort {

    @Override
    public void recordDelivery(NotificationTemplateCode template, DeliveryOutcome outcome) {
        // Functional telemetry is opt-in.
    }
}
