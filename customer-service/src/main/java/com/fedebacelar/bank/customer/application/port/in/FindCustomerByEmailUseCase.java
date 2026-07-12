package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.view.CustomerDetails;

public interface FindCustomerByEmailUseCase {
    CustomerDetails findByEmail(String email);
}
