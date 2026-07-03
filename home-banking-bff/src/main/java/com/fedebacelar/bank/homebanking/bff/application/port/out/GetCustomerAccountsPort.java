package com.fedebacelar.bank.homebanking.bff.application.port.out;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

public interface GetCustomerAccountsPort {

    List<JsonNode> getAccounts(UUID customerId, String accessToken);
}
