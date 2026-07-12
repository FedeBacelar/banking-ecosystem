package com.fedebacelar.bank.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.gateway.server.webflux.routes[0].id=test-bff",
                "spring.cloud.gateway.server.webflux.routes[0].uri=forward:/test/web",
                "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/web/**"
        }
)
@Import(SecurityConfigTest.TestRouteController.class)
class SecurityConfigTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void shouldAllowOnlyBffRoutesAtTheExternalBoundary() {
        webTestClient().get()
                .uri("/web/onboarding/status")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldDenyDirectCustomerServiceSurfaceEvenWithBearerToken() {
        webTestClient().get()
                .uri("/api/customers/123")
                .headers(headers -> headers.setBearerAuth("legacy-customer-token"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldDenyDirectAccountServiceSurfaceEvenWithBearerToken() {
        webTestClient().get()
                .uri("/api/accounts/123")
                .headers(headers -> headers.setBearerAuth("legacy-account-token"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldDenyInternalServiceEndpointsFromTheGateway() {
        webTestClient().post()
                .uri("/internal/onboarding/applications")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldExposeHealthWithoutAuthentication() {
        webTestClient().get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @RestController
    static class TestRouteController {

        @GetMapping("/test/web/**")
        void getBff() {
        }
    }
}
