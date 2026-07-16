package com.fedebacelar.bank.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "otel.traces.exporter=none",
                "otel.metrics.exporter=none",
                "otel.logs.exporter=none"
        }
)
@ActiveProfiles("observability")
class GatewayTracePropagationTest {
    private static final String INCOMING_TRACE_ID = "11111111111111111111111111111111";
    private static final String INCOMING_TRACEPARENT =
            "00-" + INCOMING_TRACE_ID + "-2222222222222222-01";
    private static final AtomicReference<String> FORWARDED_TRACEPARENT = new AtomicReference<>();
    private static final HttpServer DOWNSTREAM = startDownstream();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void gatewayRoute(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "trace-test-bff");
        registry.add(
                "spring.cloud.gateway.server.webflux.routes[0].uri",
                () -> "http://127.0.0.1:" + DOWNSTREAM.getAddress().getPort()
        );
        registry.add(
                "spring.cloud.gateway.server.webflux.routes[0].predicates[0]",
                () -> "Path=/web/**"
        );
    }

    @BeforeEach
    void clearCapturedHeader() {
        FORWARDED_TRACEPARENT.set(null);
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.stop(0);
    }

    @Test
    void shouldContinueTheIncomingW3cTraceAcrossTheGatewayProxy() {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()
                .uri("/web/trace-propagation")
                .header("traceparent", INCOMING_TRACEPARENT)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(FORWARDED_TRACEPARENT.get())
                .matches("00-" + INCOMING_TRACE_ID + "-[0-9a-f]{16}-0[13]");
        assertThat(FORWARDED_TRACEPARENT.get().split("-")[2])
                .isNotEqualTo("2222222222222222");
    }

    @Test
    void shouldCreateAndPropagateAValidTraceWhenTheBrowserHasNoTraceContext() {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()
                .uri("/web/trace-propagation")
                .exchange()
                .expectStatus().isNoContent();

        assertThat(FORWARDED_TRACEPARENT.get())
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[13]");
    }

    private static HttpServer startDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                FORWARDED_TRACEPARENT.set(exchange.getRequestHeaders().getFirst("traceparent"));
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start the trace propagation test server", exception);
        }
    }
}
