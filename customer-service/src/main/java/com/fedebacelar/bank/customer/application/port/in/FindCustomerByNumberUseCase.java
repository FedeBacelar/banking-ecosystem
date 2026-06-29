package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.view.CustomerDetails;

public interface FindCustomerByNumberUseCase {

    CustomerDetails findByCustomerNumber(String customerNumber);

}
