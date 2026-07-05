package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.security;

import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ClientCredentialsTokenAdapter implements GetInternalAccessTokenPort {

    private static final String INTERNAL_CLIENT_REGISTRATION_ID = "keycloak-service";
    private static final String INTERNAL_PRINCIPAL_NAME = "home-banking-bff";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2ClientCredentialsTokenAdapter(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public String getAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(INTERNAL_CLIENT_REGISTRATION_ID)
                .principal(INTERNAL_PRINCIPAL_NAME)
                .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null) {
            throw new IllegalStateException("Could not authorize internal BFF client.");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }
}
