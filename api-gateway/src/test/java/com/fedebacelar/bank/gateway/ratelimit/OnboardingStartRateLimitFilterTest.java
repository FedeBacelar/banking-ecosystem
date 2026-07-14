package com.fedebacelar.bank.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class OnboardingStartRateLimitFilterTest {

    @Test
    void shouldShortCircuitOnlyTheExactAnonymousStartEndpoint() {
        OnboardingStartRateLimitProperties properties = properties(true);
        OnboardingStartRateLimitFilter filter = filter(properties);
        AtomicInteger downstreamCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            downstreamCalls.incrementAndGet();
            exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
            return exchange.getResponse().setComplete();
        };

        for (int request = 0; request < 3; request++) {
            MockServerWebExchange exchange = exchange(HttpMethod.POST, "/web/onboarding/applications");
            filter.filter(exchange, chain).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        MockServerWebExchange rejected = exchange(HttpMethod.POST, "/web/onboarding/applications");
        filter.filter(rejected, chain).block();
        assertThat(rejected.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rejected.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(rejected.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(rejected.getResponse().getHeaders().getContentType().toString())
                .isEqualTo("application/problem+json");
        assertThat(responseBody(rejected))
                .contains("\"code\":\"ONBOARDING_START_RATE_LIMIT\"")
                .doesNotContain("127.0.0.1");
        assertThat(downstreamCalls).hasValue(3);

        MockServerWebExchange encodedPath = exchange(
                HttpMethod.POST,
                "/web/onboarding/%61pplications"
        );
        filter.filter(encodedPath, chain).block();
        assertThat(encodedPath.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        MockServerWebExchange matrixPath = exchange(
                HttpMethod.POST,
                "/web/onboarding/applications;attempt=1"
        );
        filter.filter(matrixPath, chain).block();
        assertThat(matrixPath.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        MockServerWebExchange differentPath = exchange(HttpMethod.POST, "/web/onboarding/magic-links/consume");
        filter.filter(differentPath, chain).block();
        MockServerWebExchange differentMethod = exchange(HttpMethod.GET, "/web/onboarding/applications");
        filter.filter(differentMethod, chain).block();
        assertThat(downstreamCalls).hasValue(5);
    }

    @Test
    void shouldPreserveOnlyASafeCorrelationId() {
        OnboardingStartRateLimitFilter filter = filter(properties(true));
        GatewayFilterChain chain = exchange -> exchange.getResponse().setComplete();
        for (int request = 0; request < 3; request++) {
            filter.filter(exchange(HttpMethod.POST, "/web/onboarding/applications"), chain).block();
        }

        MockServerWebExchange safe = exchange(HttpMethod.POST, "/web/onboarding/applications", "safe-correlation_42");
        filter.filter(safe, chain).block();
        assertThat(safe.getResponse().getHeaders().getFirst("X-Correlation-Id"))
                .isEqualTo("safe-correlation_42");
    }

    @Test
    void shouldBypassTheLimiterWhenItIsExplicitlyDisabled() {
        OnboardingStartRateLimitProperties properties = properties(false);
        OnboardingStartRateLimitFilter filter = filter(properties);
        AtomicInteger downstreamCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            downstreamCalls.incrementAndGet();
            return exchange.getResponse().setComplete();
        };

        for (int request = 0; request < 20; request++) {
            filter.filter(exchange(HttpMethod.POST, "/web/onboarding/applications"), chain).block();
        }

        assertThat(downstreamCalls).hasValue(20);
    }

    private OnboardingStartRateLimitFilter filter(OnboardingStartRateLimitProperties properties) {
        return new OnboardingStartRateLimitFilter(
                properties,
                new TrustedClientIpResolver(properties.trustedProxies()),
                new LocalOnboardingStartRateLimiter(properties, Ticker.systemTicker())
        );
    }

    private OnboardingStartRateLimitProperties properties(boolean enabled) {
        return new OnboardingStartRateLimitProperties(
                enabled,
                3,
                Duration.ofMinutes(1),
                10,
                Duration.ofHours(1),
                30,
                300,
                100,
                Duration.ofHours(2),
                64,
                ""
        );
    }

    private MockServerWebExchange exchange(HttpMethod method, String path) {
        return exchange(method, path, null);
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String correlationId) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.method(
                        method,
                        URI.create("http://localhost" + path)
                )
                .remoteAddress(new InetSocketAddress("127.0.0.1", 54_321));
        if (correlationId != null) {
            request.header("X-Correlation-Id", correlationId);
        }
        return MockServerWebExchange.from(request.build());
    }

    private String responseBody(MockServerWebExchange exchange) {
        var buffer = DataBufferUtils.join(exchange.getResponse().getBody()).block();
        assertThat(buffer).isNotNull();
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        DataBufferUtils.release(buffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
