package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.security;

import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.InternalAccessPurpose;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
public class OAuth2ClientCredentialsTokenAdapter implements GetInternalAccessTokenPort {

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2ClientCredentialsTokenAdapter(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public String getAccessToken(InternalAccessPurpose purpose) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId(purpose))
                .principal("home-banking-bff:" + purpose.name().toLowerCase())
                .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null) {
            throw new IllegalStateException("Could not authorize internal BFF client.");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    private String registrationId(InternalAccessPurpose purpose) {
        return switch (purpose) {
            case ONBOARDING -> "onboarding-service";
            case HOME_BANKING -> "home-banking-service";
        };
    }
}
