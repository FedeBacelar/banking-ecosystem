package com.fedebacelar.bank.customer.application.usecase.lifecycle;

import com.fedebacelar.bank.customer.application.command.CustomerReasonCommand;
import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.ReactivateCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.exception.CustomerNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ReactivateCustomerService implements ReactivateCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;
    private final Clock clock;

    public ReactivateCustomerService(CustomerRepositoryPort customerRepositoryPort, Clock clock) {
        this.customerRepositoryPort = customerRepositoryPort;
        this.clock = clock;
    }

    @Override
    public CustomerDetails reactivate(CustomerReasonCommand command) {
        var customer = customerRepositoryPort.findByCustomerId(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        return CustomerDetailsMapper.toDetails(customerRepositoryPort.save(customer.reactivate(command.reason(), Instant.now(clock))));
    }
}
