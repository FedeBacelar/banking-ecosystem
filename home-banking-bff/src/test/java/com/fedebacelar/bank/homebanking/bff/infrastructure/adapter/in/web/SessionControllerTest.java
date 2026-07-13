package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
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
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    @Test
    void shouldReturnProblemDetailForAnonymousApiRequest() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldValidateAuthenticatedAggregateButExposeOnlyPresentationIdentity() throws Exception {
        UUID customerId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser("keycloak-sub", "homebanking-user", "user@example.com");
        String displayName = "Home Banking User";
        ObjectMapper mapper = new ObjectMapper();
        when(getAuthenticatedHomeContextUseCase.getHomeContext(user)).thenReturn(new HomeBankingContext(
                user.subject(),
                user.username(),
                user.email(),
                customerId,
                mapper.readTree("{\"id\":\"%s\"}".formatted(customerId)),
                List.of()
        ));

        mockMvc.perform(get("/me").with(oidcLogin().idToken(token -> token
                        .subject(user.subject())
                        .claim("preferred_username", user.username())
                        .claim("email", user.email())
                        .claim("name", displayName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.username()))
                .andExpect(jsonPath("$.displayName").value(displayName))
                .andExpect(jsonPath("$.subject").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.customer").doesNotExist())
                .andExpect(jsonPath("$.accounts").doesNotExist());

        verify(getAuthenticatedHomeContextUseCase).getHomeContext(user);
    }

    @Test
    void shouldUseUsernameWhenOidcNameIsUnavailable() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser("keycloak-sub", "homebanking-user", "user@example.com");
        when(getAuthenticatedHomeContextUseCase.getHomeContext(user)).thenReturn(new HomeBankingContext(
                user.subject(), user.username(), user.email(), UUID.randomUUID(), null, List.of()
        ));

        mockMvc.perform(get("/me").with(oidcLogin().idToken(token -> token
                        .subject(user.subject())
                        .claim("preferred_username", user.username())
                        .claim("email", user.email()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(user.username()));
    }

    @Test
    void shouldStartOnlyTheFixedHomeLoginJourney() throws Exception {
        mockMvc.perform(get("/auth/login/home")
                        .queryParam("returnTo", "https://attacker.example/redirect"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void shouldRedirectOauthFailureToFixedFrontendError() throws Exception {
        mockMvc.perform(get("/login/oauth2/code/keycloak")
                        .queryParam("error", "access_denied")
                        .queryParam("state", "invalid-state"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:4200/error?reason=authentication"));
    }

    @Test
    void shouldRejectLogoutWithoutCsrf() throws Exception {
        mockMvc.perform(post("/logout").with(oidcLogin().clientRegistration(
                        clientRegistrationRepository.findByRegistrationId("keycloak")
                )))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptTopLevelFormLogoutAndUseFixedPostLogoutDestination() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(oidcLogin().clientRegistration(
                                clientRegistrationRepository.findByRegistrationId("keycloak")
                        ))
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:4200/sesion-cerrada"));
    }

    @Test
    void shouldNotExposeRedundantSessionEndpoint() throws Exception {
        mockMvc.perform(get("/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }
}
