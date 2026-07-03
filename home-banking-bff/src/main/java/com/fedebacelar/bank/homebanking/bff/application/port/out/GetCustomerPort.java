package com.fedebacelar.bank.homebanking.bff.application.port.out;

import tools.jackson.databind.JsonNode;
import java.util.UUID;

public interface GetCustomerPort {

    JsonNode getCustomer(UUID customerId, String accessToken);
}
