package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.account;

import tools.jackson.databind.JsonNode;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetCustomerAccountsPort;
import java.util.List;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AccountServiceAdapter implements GetCustomerAccountsPort {

    private final WebClient webClient;

    public AccountServiceAdapter(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.build();
    }

    @Override
    public List<JsonNode> getAccounts(UUID customerId, String accessToken) {
        return webClient.get()
                .uri("http://account-service/accounts/customer/{customerId}", customerId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<JsonNode>>() {
                })
                .block();
    }
}
