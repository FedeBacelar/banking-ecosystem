package com.fedebacelar.bank.homebanking.bff.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootTest(properties = {
        "otel.traces.exporter=none",
        "otel.metrics.exporter=none",
        "otel.logs.exporter=none",
        "spring.security.oauth2.client.registration.keycloak.client-id=home-banking-bff",
        "spring.security.oauth2.client.registration.keycloak.client-secret=local-bff-secret",
        "spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.keycloak.redirect-uri=http://localhost:8085/web/login/oauth2/code/keycloak",
        "spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email",
        "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/auth",
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/token",
        "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/certs",
        "spring.security.oauth2.client.provider.keycloak.user-info-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/userinfo",
        "spring.security.oauth2.client.provider.keycloak.user-name-attribute=sub"
})
@ActiveProfiles("observability")
class WebClientObservabilityTest {

    @Autowired
    private WebClient internalServiceWebClient;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter;

    @BeforeEach
    void passRequestsThroughTheTestLoadBalancer() {
        when(loadBalancerFilter.filter(any(), any())).thenAnswer(invocation -> {
            ClientRequest request = invocation.getArgument(0);
            ExchangeFunction next = invocation.getArgument(1);
            return next.exchange(request);
        });
    }

    @Test
    void shouldPreserveLoadBalancingCorrelationAndW3cTracePropagation() {
        assertThat(environment.getProperty("management.prometheus.metrics.export.enabled", Boolean.class))
                .isTrue();
        assertThat(applicationContext.getBeansOfType(PrometheusMeterRegistry.class)).hasSize(1);
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        WebClient client = internalServiceWebClient.mutate()
                .exchangeFunction(request -> {
                    capturedRequest.set(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
                })
                .build();
        Span span = openTelemetry.getTracer("nerva-test").spanBuilder("bff-outbound-call").startSpan();

        MDC.put("correlationId", "correlation-test-01");
        try (Scope ignored = span.makeCurrent()) {
            client.get()
                    .uri("http://onboarding-service/internal/observability-test")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } finally {
            MDC.remove("correlationId");
            span.end();
        }

        ClientRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.headers().getFirst("X-Correlation-Id")).isEqualTo("correlation-test-01");
        assertThat(request.headers().getFirst("traceparent"))
                .matches("00-" + span.getSpanContext().getTraceId() + "-[0-9a-f]{16}-0[13]");
        verify(loadBalancerFilter).filter(any(), any());
    }
}
