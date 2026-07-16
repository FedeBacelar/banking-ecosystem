package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.customer;

import tools.jackson.databind.JsonNode;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetCustomerPort;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CustomerServiceAdapter implements GetCustomerPort {

    private final WebClient webClient;

    public CustomerServiceAdapter(WebClient internalServiceWebClient) {
        this.webClient = internalServiceWebClient;
    }

    @Override
    public JsonNode getCustomer(UUID customerId, String accessToken) {
        return webClient.get()
                .uri("http://customer-service/customers/{customerId}", customerId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}
