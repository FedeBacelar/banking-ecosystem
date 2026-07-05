package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.homebanking.bff.application.port.in.ConsumeOnboardingMagicLinkUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingSessionUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
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
class OnboardingControllerTest {

    private static final String CONTINUATION_COOKIE = "NB_ONBOARDING_CONTINUATION";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartOnboardingApplicationUseCase startOnboardingApplicationUseCase;

    @MockitoBean
    private ConsumeOnboardingMagicLinkUseCase consumeOnboardingMagicLinkUseCase;

    @MockitoBean
    private GetOnboardingSessionUseCase getOnboardingSessionUseCase;

    @MockitoBean
    private GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    @Test
    void shouldStartOnboardingApplicationWithoutLogin() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T20:00:00Z");

        when(startOnboardingApplicationUseCase.startApplication("applicant@example.com"))
                .thenReturn(new OnboardingApplication(
                        applicationId,
                        "applicant@example.com",
                        "EMAIL_VERIFICATION_PENDING",
                        now.plusSeconds(1800),
                        null,
                        null,
                        now.plusSeconds(86400),
                        now,
                        now
                ));

        mockMvc.perform(post("/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"applicant@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.email").value("applicant@example.com"))
                .andExpect(jsonPath("$.status").value("EMAIL_VERIFICATION_PENDING"));
    }

    @Test
    void shouldConsumeMagicLinkAndSetContinuationCookie() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(7200);

        when(consumeOnboardingMagicLinkUseCase.consumeMagicLink("magic-token"))
                .thenReturn(new OnboardingContinuation(applicationId, "IN_PROGRESS", "continuation-token", expiresAt));

        mockMvc.perform(post("/onboarding/magic-links/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"magic-token\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().value(CONTINUATION_COOKIE, "continuation-token"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Path=/web/onboarding")))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldExposeAnonymousOnboardingSessionWhenCookieIsMissing() throws Exception {
        when(getOnboardingSessionUseCase.getSession(null)).thenReturn(OnboardingSession.anonymous());

        mockMvc.perform(get("/onboarding/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldValidateOnboardingSessionFromCookie() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(7200);

        when(getOnboardingSessionUseCase.getSession("continuation-token"))
                .thenReturn(OnboardingSession.active(applicationId, "IN_PROGRESS", expiresAt));

        mockMvc.perform(get("/onboarding/session")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldClearOnboardingSessionCookie() throws Exception {
        mockMvc.perform(delete("/onboarding/session"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(CONTINUATION_COOKIE, 0))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Path=/web/onboarding")));
    }
}
