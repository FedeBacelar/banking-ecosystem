package com.fedebacelar.bank.account.infrastructure.adapter.out.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.account.TestcontainersConfiguration;
import com.fedebacelar.bank.account.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.account.infrastructure.config.W3cFeignTracePropagationInterceptor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
class CustomerFeignTracePropagationIntegrationTest {

    private static final AtomicReference<String> RECEIVED_TRACEPARENT = new AtomicReference<>();
    private static final AtomicReference<String> RECEIVED_AUTHORIZATION = new AtomicReference<>();
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final HttpServer SERVER = startServer();

    @Autowired
    private CustomerFeignClient customerClient;

    @Autowired
    private W3cFeignTracePropagationInterceptor tracePropagationInterceptor;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private GetInternalAccessTokenPort accessTokenPort;

    @DynamicPropertySource
    static void customerUrl(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.cloud.openfeign.client.config.customer-service.url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort()
        );
    }

    @BeforeEach
    void setUp() {
        RECEIVED_TRACEPARENT.set(null);
        RECEIVED_AUTHORIZATION.set(null);
        when(accessTokenPort.getAccessToken()).thenReturn("account-service-token");
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void propagatesW3cContextAndUsesOnlyTheTechnicalToken() {
        assertThat(tracePropagationInterceptor).isNotNull();
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).hasSize(1);
        Span parent = openTelemetry.getTracer("account-feign-propagation-test")
                .spanBuilder("test-parent")
                .startSpan();

        try (Scope ignored = parent.makeCurrent()) {
            assertThat(customerClient.getCustomerById(CUSTOMER_ID).customerId()).isEqualTo(CUSTOMER_ID);
        } finally {
            parent.end();
        }

        String[] traceparent = RECEIVED_TRACEPARENT.get().split("-");
        assertThat(traceparent).hasSize(4);
        assertThat(traceparent[0]).isEqualTo("00");
        assertThat(traceparent[1]).isEqualTo(parent.getSpanContext().getTraceId());
        assertThat(RECEIVED_AUTHORIZATION.get()).isEqualTo("Bearer account-service-token");
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/customers/", CustomerFeignTracePropagationIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not start the customer propagation test server", failure);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        RECEIVED_TRACEPARENT.set(exchange.getRequestHeaders().getFirst("traceparent"));
        RECEIVED_AUTHORIZATION.set(exchange.getRequestHeaders().getFirst("Authorization"));
        byte[] body = ("{\"customerId\":\"" + CUSTOMER_ID
                + "\",\"customerNumber\":\"CUS-000001\",\"status\":\"ACTIVE\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
