package com.fedebacelar.bank.customer.application.usecase.lifecycle;

import com.fedebacelar.bank.customer.application.command.CustomerReasonCommand;
import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.RejectCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.exception.CustomerNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class RejectCustomerKycService implements RejectCustomerKycUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;
    private final Clock clock;

    public RejectCustomerKycService(CustomerRepositoryPort customerRepositoryPort, Clock clock) {
        this.customerRepositoryPort = customerRepositoryPort;
        this.clock = clock;
    }

    @Override
    public CustomerDetails rejectKyc(CustomerReasonCommand command) {
        var customer = customerRepositoryPort.findByCustomerId(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        return CustomerDetailsMapper.toDetails(customerRepositoryPort.save(customer.rejectKyc(command.reason(), Instant.now(clock))));
    }
}
