package com.fedebacelar.bank.customer.application.usecase.query;

import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.GetCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.exception.CustomerNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetCustomerService implements GetCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public GetCustomerService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public CustomerDetails getCustomer(UUID customerId) {
        return customerRepositoryPort.findByCustomerId(customerId)
                .map(CustomerDetailsMapper::toDetails)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
