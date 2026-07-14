package com.fedebacelar.bank.account.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class OAuth2ClientCredentialsTokenAdapterTest {

    private final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
    private final OAuth2ClientCredentialsTokenAdapter adapter = new OAuth2ClientCredentialsTokenAdapter(manager);

    @Test
    void obtainsTheTokenFromTheDedicatedAccountServiceRegistration() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "account-service-token",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);

        String token = adapter.getAccessToken();

        assertThat(token).isEqualTo("account-service-token");
        ArgumentCaptor<OAuth2AuthorizeRequest> request = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        verify(manager).authorize(request.capture());
        assertThat(request.getValue().getClientRegistrationId()).isEqualTo("account-service");
        assertThat(request.getValue().getPrincipal().getName()).isEqualTo("account-service");
    }

    @Test
    void failsClosedWhenAuthorizationIsUnavailable() {
        when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        assertThatThrownBy(adapter::getAccessToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not authorize account service client.");
    }
}
