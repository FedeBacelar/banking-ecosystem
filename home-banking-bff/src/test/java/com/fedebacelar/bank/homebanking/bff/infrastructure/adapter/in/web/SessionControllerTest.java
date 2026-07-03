package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
        "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/auth",
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/token",
        "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/certs",
        "spring.security.oauth2.client.provider.keycloak.user-info-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/userinfo",
        "spring.security.oauth2.client.provider.keycloak.user-name-attribute=sub"
})
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    @Test
    void shouldRedirectAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void shouldExposeAnonymousSession() throws Exception {
        mockMvc.perform(get("/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void shouldExposeAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/session")
                        .with(oidcLogin().idToken(token -> token
                                .subject("keycloak-sub")
                                .claim("preferred_username", "homebanking-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.subject").value("keycloak-sub"))
                .andExpect(jsonPath("$.username").value("homebanking-user"));
    }
}
