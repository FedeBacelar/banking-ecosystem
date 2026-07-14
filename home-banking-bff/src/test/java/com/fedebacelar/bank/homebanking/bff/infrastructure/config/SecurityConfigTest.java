package com.fedebacelar.bank.homebanking.bff.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void shouldAlwaysUseFixedHomeDestinationAfterAuthentication() throws Exception {
        AuthenticationSuccessHandler handler = securityConfig.authenticationSuccessHandler(
                "http://localhost:4200/app/inicio",
                "http://localhost:4200/onboarding/finalizando"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("returnTo", "https://attacker.example/redirect");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                request,
                response,
                UsernamePasswordAuthenticationToken.authenticated("user", "n/a", List.of())
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:4200/app/inicio");
    }

    @Test
    void shouldUseTheFixedCompletionDestinationForTheDedicatedRegistration() throws Exception {
        AuthenticationSuccessHandler handler = securityConfig.authenticationSuccessHandler(
                "http://localhost:4200/app/inicio",
                "http://localhost:4200/onboarding/finalizando"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("returnTo", "https://attacker.example/redirect");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of(IdTokenClaimNames.SUB, "subject")
        );
        DefaultOidcUser user = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("OIDC_USER")),
                idToken
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                user,
                user.getAuthorities(),
                "keycloak-onboarding-completion"
        );

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:4200/onboarding/finalizando");
    }

    @Test
    void shouldUseFixedFrontendDestinationAfterAuthenticationFailure() throws Exception {
        AuthenticationFailureHandler handler = securityConfig.authenticationFailureHandler(
                "http://localhost:4200/error?reason=authentication"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("returnTo", "https://attacker.example/redirect");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("invalid"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:4200/error?reason=authentication");
    }

    @Test
    void shouldInitiateOidcLogoutWithFixedPostLogoutDestination() throws Exception {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("home-banking-bff")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8085/web/login/oauth2/code/keycloak")
                .scope("openid")
                .authorizationUri("http://localhost:8090/realms/banking/protocol/openid-connect/auth")
                .tokenUri("http://localhost:8090/realms/banking/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:8090/realms/banking/protocol/openid-connect/certs")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint",
                        "http://localhost:8090/realms/banking/protocol/openid-connect/logout"
                ))
                .clientName("Keycloak")
                .build();
        LogoutSuccessHandler handler = securityConfig.logoutSuccessHandler(
                new InMemoryClientRegistrationRepository(registration),
                "http://localhost:4200/sesion-cerrada"
        );
        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of(IdTokenClaimNames.SUB, "subject")
        );
        DefaultOidcUser user = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("OIDC_USER")),
                idToken
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                user,
                user.getAuthorities(),
                registration.getRegistrationId()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/web/logout");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8085);
        request.setContextPath("/web");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onLogoutSuccess(request, response, authentication);

        String redirect = URLDecoder.decode(response.getRedirectedUrl(), StandardCharsets.UTF_8);
        assertThat(redirect)
                .startsWith("http://localhost:8090/realms/banking/protocol/openid-connect/logout?")
                .contains("id_token_hint=id-token")
                .contains("post_logout_redirect_uri=http://localhost:4200/sesion-cerrada");
    }
}
