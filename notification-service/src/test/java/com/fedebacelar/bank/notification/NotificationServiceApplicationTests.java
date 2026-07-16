package com.fedebacelar.bank.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort;
import com.fedebacelar.bank.notification.infrastructure.adapter.out.observability.NoOpNotificationTelemetryAdapter;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "notification.email.from=no-reply@nerva.local",
        "notification.action-links.onboarding-allowed-origins=http://localhost:4200",
        "notification.action-links.keycloak-allowed-origins=http://localhost:8090",
        "spring.mail.host=localhost",
        "spring.mail.port=2525"
})
@Import(TestcontainersConfiguration.class)
class NotificationServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private NotificationTelemetryPort telemetry;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Test
    void contextLoadsWithObservabilityDisabledByDefault() {
        assertThat(telemetry).isInstanceOf(NoOpNotificationTelemetryAdapter.class);
        assertThat(openTelemetry).isSameAs(OpenTelemetry.noop());
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).isEmpty();
    }
}
