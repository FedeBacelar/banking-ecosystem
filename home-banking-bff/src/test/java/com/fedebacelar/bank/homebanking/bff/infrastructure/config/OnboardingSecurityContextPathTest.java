package com.fedebacelar.bank.homebanking.bff.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.application.usecase.OnboardingFlowService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        "spring.security.oauth2.client.registration.keycloak-service.provider=keycloak",
        "spring.security.oauth2.client.registration.keycloak-service.client-id=home-banking-bff",
        "spring.security.oauth2.client.registration.keycloak-service.client-secret=local-bff-secret",
        "spring.security.oauth2.client.registration.keycloak-service.authorization-grant-type=client_credentials",
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
    void shouldNotRedirectInvalidOnboardingMutationToKeycloakWhenBffRunsWithWebContextPath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/web/onboarding/applicant-data".formatted(port)))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.body()).contains("VALIDATION_ERROR");
    }

    @Test
    void shouldNotRedirectMalformedOnboardingMutationToKeycloakWhenBffRunsWithWebContextPath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/web/onboarding/applicant-data".formatted(port)))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{"))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.body()).contains("INVALID_REQUEST_BODY");
    }

    @Test
    void shouldNotRedirectOnboardingMutationWithoutContinuationCookieToKeycloakWhenBffRunsWithWebContextPath()
            throws Exception {
        when(onboardingFlowService.saveApplicantData(eq(null), any()))
                .thenThrow(new OnboardingSessionRequiredException());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/web/onboarding/applicant-data".formatted(port)))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""
                        {
                          "firstName": "Federico",
                          "lastName": "Bacelar",
                          "birthDate": "1990-05-10",
                          "nationality": "AR",
                          "documentType": "DNI",
                          "documentNumber": "12345678",
                          "documentIssuingCountry": "AR",
                          "phoneNumber": "+5491122223333",
                          "street": "Av Siempre Viva",
                          "streetNumber": "742",
                          "city": "Buenos Aires",
                          "province": "Buenos Aires",
                          "postalCode": "1000",
                          "country": "AR"
                        }
                        """))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.body()).contains("ONBOARDING_SESSION_REQUIRED");
    }
}
