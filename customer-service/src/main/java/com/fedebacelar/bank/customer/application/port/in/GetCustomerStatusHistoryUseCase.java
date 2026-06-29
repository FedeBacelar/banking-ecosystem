package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.view.CustomerStatusHistoryDetails;
import java.util.List;
import java.util.UUID;

public interface GetCustomerStatusHistoryUseCase {

    List<CustomerStatusHistoryDetails> getStatusHistory(UUID customerId);

}
