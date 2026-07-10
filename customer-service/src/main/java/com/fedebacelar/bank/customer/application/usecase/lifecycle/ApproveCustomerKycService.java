package com.fedebacelar.bank.customer.application.usecase.lifecycle;

import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.ApproveCustomerKycUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.exception.CustomerNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ApproveCustomerKycService implements ApproveCustomerKycUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;
    private final Clock clock;

    public ApproveCustomerKycService(CustomerRepositoryPort customerRepositoryPort, Clock clock) {
        this.customerRepositoryPort = customerRepositoryPort;
        this.clock = clock;
    }

    @Override
    public CustomerDetails approveKyc(UUID customerId, String reasonCode, String changedBy) {
        var customer = customerRepositoryPort.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        return CustomerDetailsMapper.toDetails(customerRepositoryPort.save(
                customer.approveKyc(reasonCode, changedBy, Instant.now(clock))
        ));
    }
}
