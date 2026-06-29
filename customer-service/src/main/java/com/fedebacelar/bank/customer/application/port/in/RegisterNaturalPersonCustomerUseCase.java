package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.command.RegisterNaturalPersonCustomerCommand;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;

public interface RegisterNaturalPersonCustomerUseCase {

    CustomerDetails register(RegisterNaturalPersonCustomerCommand command);

}
