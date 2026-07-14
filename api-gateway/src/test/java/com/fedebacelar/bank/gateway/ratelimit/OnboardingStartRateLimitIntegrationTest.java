package com.fedebacelar.bank.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.gateway.server.webflux.routes[0].id=rate-limit-test",
                "spring.cloud.gateway.server.webflux.routes[0].uri=forward:/test/web",
                "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/web/**",
                "banking.gateway.rate-limit.onboarding-start.short-window-requests=3",
                "banking.gateway.rate-limit.onboarding-start.short-window=PT1H",
                "banking.gateway.rate-limit.onboarding-start.long-window-requests=10",
                "banking.gateway.rate-limit.onboarding-start.long-window=PT2H",
                "banking.gateway.rate-limit.onboarding-start.client-idle-expiration=PT3H",
                "banking.gateway.rate-limit.onboarding-start.trusted-proxies="
        }
)
@Import(OnboardingStartRateLimitIntegrationTest.TestRouteController.class)
@DirtiesContext
class OnboardingStartRateLimitIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void resetDownstreamCounter() {
        TestRouteController.DOWNSTREAM_CALLS.set(0);
    }

    @Test
    void shouldLimitThePublicRouteWithoutTrustingSpoofedForwardedHeaders() {
        WebTestClient client = client();
        for (int request = 0; request < 3; request++) {
            client.post()
                    .uri("/web/onboarding/applications")
                    .header("X-Forwarded-For", "198.51.100." + (request + 1))
                    .exchange()
                    .expectStatus().isAccepted();
        }

        client.post()
                .uri("/web/onboarding/applications")
                .header("X-Forwarded-For", "203.0.113.250")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "3600")
                .expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "no-store")
                .expectHeader().contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.code").isEqualTo("ONBOARDING_START_RATE_LIMIT")
                .jsonPath("$.status").isEqualTo(429)
                .jsonPath("$.client").doesNotExist()
                .jsonPath("$.email").doesNotExist();

        client.post()
                .uri(URI.create("http://localhost:" + port + "/web/onboarding/%61pplications"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        client.post()
                .uri(URI.create("http://localhost:" + port + "/web/onboarding/applications;attempt=1"))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(TestRouteController.DOWNSTREAM_CALLS).hasValue(3);

        client.post()
                .uri("/web/onboarding/magic-links/consume")
                .exchange()
                .expectStatus().isAccepted();
        client.get()
                .uri("/web/onboarding/applications")
                .exchange()
                .expectStatus().isOk();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @RestController
    static class TestRouteController {

        private static final AtomicInteger DOWNSTREAM_CALLS = new AtomicInteger();

        @PostMapping("/test/web/**")
        @ResponseStatus(HttpStatus.ACCEPTED)
        void post() {
            DOWNSTREAM_CALLS.incrementAndGet();
        }

        @GetMapping("/test/web/**")
        void get() {
        }
    }
}
