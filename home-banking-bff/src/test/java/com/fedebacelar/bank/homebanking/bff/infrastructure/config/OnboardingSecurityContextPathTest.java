package com.fedebacelar.bank.homebanking.bff.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.application.usecase.OnboardingFlowService;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "server.servlet.context-path=/web",
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
class OnboardingSecurityContextPathTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private OnboardingFlowService onboardingFlowService;

    @MockitoBean
    private GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    @Test
    void shouldAllowPublicStartWithoutCsrfThroughWebContextPath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/web/onboarding/applications"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"applicant@example.com\"}"))
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    @Test
    void shouldRejectCookieMutationWithoutCsrfWithoutRedirectingToKeycloak() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/web/onboarding/credential-invitations/resend"))
                .header("Cookie", "NB_ONBOARDING_CONTINUATION=continuation-token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    @Test
    void shouldIssueCsrfCookieDuringMagicLinkExchangeAndReturnApiErrorForMissingContinuation() throws Exception {
        when(onboardingFlowService.consumeMagicLink("magic-token")).thenReturn(new OnboardingContinuation(
                UUID.randomUUID(),
                OnboardingState.IN_PROGRESS,
                "continuation-token",
                Instant.now().plusSeconds(7200)
        ));
        when(onboardingFlowService.resendCredentialInvitation(null, "onboarding-resend-01"))
                .thenThrow(new OnboardingSessionRequiredException());

        CsrfCookie csrf = exchangeMagicLink();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/web/onboarding/credential-invitations/resend"))
                .header("Cookie", "NB-XSRF-TOKEN=" + csrf.value())
                .header("X-XSRF-TOKEN", csrf.value())
                .header("Idempotency-Key", "onboarding-resend-01")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.body()).contains("ONBOARDING_SESSION_REQUIRED");
    }

    @Test
    void shouldNotExposeStandaloneCsrfEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(uri("/web/csrf")).GET().build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.body()).contains("AUTHENTICATION_REQUIRED");
    }

    @Test
    void shouldExposeFixedLoginJourneyThroughWebContextPath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/web/auth/login/home?returnTo=https://attacker.example/redirect"))
                .GET()
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        assertThat(response.headers().firstValue("Location"))
                .contains("/web/oauth2/authorization/keycloak");
    }

    private CsrfCookie exchangeMagicLink() throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/web/onboarding/magic-links/consume"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"token\":\"magic-token\"}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        HttpCookie cookie = cookies.getCookieStore().getCookies().stream()
                .filter(candidate -> "NB-XSRF-TOKEN".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(cookie.getPath()).isEqualTo("/");
        return new CsrfCookie(cookie.getValue());
    }

    private HttpClient client() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:%d%s".formatted(port, path));
    }

    private record CsrfCookie(String value) {
    }
}
