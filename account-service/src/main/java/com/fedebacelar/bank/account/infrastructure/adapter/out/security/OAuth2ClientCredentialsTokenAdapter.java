package com.fedebacelar.bank.account.infrastructure.adapter.out.security;

import com.fedebacelar.bank.account.application.port.out.GetInternalAccessTokenPort;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ClientCredentialsTokenAdapter implements GetInternalAccessTokenPort {
    private static final String REGISTRATION_ID = "account-service";
    private static final String PRINCIPAL = "account-service";

    private final OAuth2AuthorizedClientManager manager;

    public OAuth2ClientCredentialsTokenAdapter(OAuth2AuthorizedClientManager manager) {
        this.manager = manager;
    }

    @Override
    public String getAccessToken() {
        OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal(PRINCIPAL)
                .build());
        if (client == null) {
            throw new IllegalStateException("Could not authorize account service client.");
        }
        return client.getAccessToken().getTokenValue();
    }
}
