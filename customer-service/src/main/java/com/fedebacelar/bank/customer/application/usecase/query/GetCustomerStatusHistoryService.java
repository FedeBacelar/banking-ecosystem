package com.fedebacelar.bank.customer.application.usecase.query;

import com.fedebacelar.bank.customer.application.mapper.CustomerStatusHistoryMapper;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerStatusHistoryUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerStatusHistoryDetails;
import com.fedebacelar.bank.customer.domain.exception.CustomerNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetCustomerStatusHistoryService implements GetCustomerStatusHistoryUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public GetCustomerStatusHistoryService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public List<CustomerStatusHistoryDetails> getStatusHistory(UUID customerId) {
        if (customerRepositoryPort.findByCustomerId(customerId).isEmpty()) {
            throw new CustomerNotFoundException(customerId);
        }
        return customerRepositoryPort.findStatusHistory(customerId).stream()
                .map(CustomerStatusHistoryMapper::toDetails)
                .toList();
    }
}
