package com.fedebacelar.bank.notification.application.port.out;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.util.Locale;

public interface NotificationTelemetryPort {

    enum DeliveryOutcome {
        SENT,
        FAILED,
        REPLAYED;

        public String metricValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    void recordDelivery(NotificationTemplateCode template, DeliveryOutcome outcome);
}
