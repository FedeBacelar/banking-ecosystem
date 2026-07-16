package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.identity;

import com.fedebacelar.bank.homebanking.bff.application.exception.IdentityNotLinkedException;
import com.fedebacelar.bank.homebanking.bff.application.port.out.ResolveIdentityLinkPort;
import com.fedebacelar.bank.homebanking.bff.domain.model.IdentityLink;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.identity.dto.IdentityLinkResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class IdentityServiceAdapter implements ResolveIdentityLinkPort {

    private final WebClient webClient;

    public IdentityServiceAdapter(WebClient internalServiceWebClient) {
        this.webClient = internalServiceWebClient;
    }

    @Override
    public IdentityLink resolveByKeycloakSubject(String providerSubject, String accessToken) {
        IdentityLinkResponse response = webClient.get()
                .uri("http://identity-service/identity-links/providers/KEYCLOAK/subjects/{providerSubject}", providerSubject)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(IdentityLinkResponse.class)
                .onErrorMap(
                        WebClientResponseException.NotFound.class,
                        exception -> new IdentityNotLinkedException(providerSubject)
                )
                .block();

        return new IdentityLink(response.customerId());
    }
}
