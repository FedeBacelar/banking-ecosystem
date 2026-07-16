package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.TestcontainersConfiguration;
import com.fedebacelar.bank.onboarding.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.onboarding.infrastructure.config.W3cFeignTracePropagationInterceptor;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto.NotificationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto.SendEmailNotificationRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "otel.traces.exporter=none",
        "otel.metrics.exporter=none",
        "otel.logs.exporter=none"
})
@ActiveProfiles("observability")
@Import(TestcontainersConfiguration.class)
class FeignTracePropagationIntegrationTest {

    private static final AtomicReference<String> RECEIVED_TRACEPARENT = new AtomicReference<>();
    private static final HttpServer SERVER = startServer();

    @Autowired
    private NotificationFeignClient notificationClient;

    @Autowired
    private W3cFeignTracePropagationInterceptor tracePropagationInterceptor;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private GetInternalAccessTokenPort accessTokenPort;

    @DynamicPropertySource
    static void notificationUrl(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.cloud.openfeign.client.config.notification-service.url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort()
        );
    }

    @BeforeEach
    void setUp() {
        RECEIVED_TRACEPARENT.set(null);
        when(accessTokenPort.getAccessToken()).thenReturn("test-internal-token");
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void propagatesTheCurrentTraceAsW3cTraceContextWhenObservabilityIsEnabled() {
        assertThat(tracePropagationInterceptor).isNotNull();
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).hasSize(1);

        Span parent = openTelemetry.getTracer("onboarding-feign-propagation-test")
                .spanBuilder("test-parent")
                .startSpan();
        NotificationResponse response;
        try (Scope ignored = parent.makeCurrent()) {
            response = notificationClient.sendEmail(new SendEmailNotificationRequest(
                    "test@example.invalid",
                    "ONBOARDING_EMAIL_MAGIC_LINK",
                    Map.of("test", "value"),
                    "test-correlation",
                    true
            ));
        } finally {
            parent.end();
        }

        assertThat(response.status()).isEqualTo("SENT");
        String[] traceparent = RECEIVED_TRACEPARENT.get().split("-");
        assertThat(traceparent).hasSize(4);
        assertThat(traceparent[0]).isEqualTo("00");
        assertThat(traceparent[1]).isEqualTo(parent.getSpanContext().getTraceId());
        assertThat(traceparent[2]).isEqualTo(parent.getSpanContext().getSpanId());
        assertThat(traceparent[3]).isEqualTo(parent.getSpanContext().getTraceFlags().asHex());
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/notifications/email", FeignTracePropagationIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not start the propagation test server", failure);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        RECEIVED_TRACEPARENT.set(exchange.getRequestHeaders().getFirst("traceparent"));
        exchange.getRequestBody().readAllBytes();
        byte[] body = "{\"status\":\"SENT\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
