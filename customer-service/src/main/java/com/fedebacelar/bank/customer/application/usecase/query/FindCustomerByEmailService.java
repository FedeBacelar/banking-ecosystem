package com.fedebacelar.bank.customer.application.usecase.query;

import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByEmailUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.exception.CustomerEmailNotFoundException;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class FindCustomerByEmailService implements FindCustomerByEmailUseCase {
    private final CustomerRepositoryPort customerRepositoryPort;

    public FindCustomerByEmailService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public CustomerDetails findByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return customerRepositoryPort.findByEmail(normalizedEmail)
                .map(CustomerDetailsMapper::toDetails)
                .orElseThrow(() -> new CustomerEmailNotFoundException(normalizedEmail));
    }
}
