package com.fedebacelar.bank.notification.infrastructure.adapter.out.observability;

import com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("observability")
public class MicrometerNotificationTelemetryAdapter implements NotificationTelemetryPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerNotificationTelemetryAdapter.class);

    private final Map<NotificationTemplateCode, Map<DeliveryOutcome, Counter>> counters;

    public MicrometerNotificationTelemetryAdapter(MeterRegistry meterRegistry) {
        this.counters = registerCounters(meterRegistry);
    }

    @Override
    public void recordDelivery(NotificationTemplateCode template, DeliveryOutcome outcome) {
        try {
            counters.get(template).get(outcome).increment();
            LOGGER.atInfo()
                    .addKeyValue("event.name", "notification.email." + outcome.metricValue())
                    .addKeyValue("notification.template", templateValue(template))
                    .addKeyValue("delivery.outcome", outcome.metricValue())
                    .log("Email delivery event");
        } catch (RuntimeException ignored) {
            // Telemetry must never affect notification delivery.
        }
    }

    private Map<NotificationTemplateCode, Map<DeliveryOutcome, Counter>> registerCounters(
            MeterRegistry meterRegistry
    ) {
        Map<NotificationTemplateCode, Map<DeliveryOutcome, Counter>> registered =
                new EnumMap<>(NotificationTemplateCode.class);
        for (NotificationTemplateCode template : NotificationTemplateCode.values()) {
            Map<DeliveryOutcome, Counter> outcomes = new EnumMap<>(DeliveryOutcome.class);
            for (DeliveryOutcome outcome : DeliveryOutcome.values()) {
                outcomes.put(outcome, Counter.builder("nerva.notification.deliveries")
                        .description("Email notification delivery outcomes")
                        .tag("template", templateValue(template))
                        .tag("outcome", outcome.metricValue())
                        .register(meterRegistry));
            }
            registered.put(template, Map.copyOf(outcomes));
        }
        return Map.copyOf(registered);
    }

    private String templateValue(NotificationTemplateCode template) {
        return template.name().toLowerCase(Locale.ROOT);
    }
}
