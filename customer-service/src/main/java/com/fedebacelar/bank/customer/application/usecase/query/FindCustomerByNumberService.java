package com.fedebacelar.bank.customer.application.usecase.query;

import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByNumberUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.exception.CustomerNumberNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FindCustomerByNumberService implements FindCustomerByNumberUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public FindCustomerByNumberService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public CustomerDetails findByCustomerNumber(String customerNumber) {
        return customerRepositoryPort.findByCustomerNumber(customerNumber)
                .map(CustomerDetailsMapper::toDetails)
                .orElseThrow(() -> new CustomerNumberNotFoundException(customerNumber));
    }
}
