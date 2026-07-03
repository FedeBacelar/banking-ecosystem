package com.fedebacelar.bank.gateway.security;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.gateway.server.webflux.routes[0].id=test-customers",
                "spring.cloud.gateway.server.webflux.routes[0].uri=forward:/test/customers",
                "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/customers/**",
                "spring.cloud.gateway.server.webflux.routes[1].id=test-accounts",
                "spring.cloud.gateway.server.webflux.routes[1].uri=forward:/test/accounts",
                "spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/accounts/**",
                "spring.cloud.gateway.server.webflux.routes[2].id=test-bff",
                "spring.cloud.gateway.server.webflux.routes[2].uri=forward:/test/web",
                "spring.cloud.gateway.server.webflux.routes[2].predicates[0]=Path=/web/**"
        }
)
@Import(SecurityConfigTest.TestRouteController.class)
class SecurityConfigTest {

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void shouldRejectRequestWithoutToken() {
        webTestClient().get()
                .uri("/api/customers/123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowCustomerReadWithCustomerReadRole() {
        String token = tokenWithRoles("CUSTOMER_READ");

        webTestClient()
                .get()
                .uri("/api/customers/123")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldRejectCustomerReadWithoutCustomerReadRole() {
        String token = tokenWithRoles("ACCOUNT_READ");

        webTestClient()
                .get()
                .uri("/api/customers/123")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldAllowCustomerWriteWithCustomerWriteRole() {
        String token = tokenWithRoles("CUSTOMER_WRITE");

        webTestClient()
                .post()
                .uri("/api/customers/natural-persons")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldAllowAccountReadWithAccountReadRole() {
        String token = tokenWithRoles("ACCOUNT_READ");

        webTestClient()
                .get()
                .uri("/api/accounts/123")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldAllowAccountWriteWithAccountWriteRole() {
        String token = tokenWithRoles("ACCOUNT_WRITE");

        webTestClient()
                .patch()
                .uri("/api/accounts/123/freeze")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldRejectUnsupportedCustomerMethodEvenWithWriteRole() {
        String token = tokenWithRoles("CUSTOMER_WRITE");

        webTestClient()
                .delete()
                .uri("/api/customers/123")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldExposeHealthWithoutToken() {
        webTestClient().get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldAllowBffRoutesWithoutBearerToken() {
        webTestClient().get()
                .uri("/web/session")
                .exchange()
                .expectStatus().isOk();
    }

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private String tokenWithRoles(String... roles) {
        String token = String.join("-", roles).toLowerCase() + "-token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer("http://localhost:8090/realms/banking-ecosystem")
                .subject("api-tester")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();

        when(jwtDecoder.decode(token)).thenReturn(Mono.just(jwt));
        return token;
    }

    @RestController
    static class TestRouteController {

        @GetMapping("/test/customers/**")
        void getCustomer() {
        }

        @PostMapping("/test/customers/**")
        void postCustomer() {
        }

        @GetMapping("/test/accounts/**")
        void getAccount() {
        }

        @PatchMapping("/test/accounts/**")
        void patchAccount() {
        }

        @GetMapping("/test/web/**")
        void getBff() {
        }
    }
}
