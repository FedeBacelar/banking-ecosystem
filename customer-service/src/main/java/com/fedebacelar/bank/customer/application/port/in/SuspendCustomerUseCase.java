package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.command.CustomerReasonCommand;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;

public interface SuspendCustomerUseCase {

    CustomerDetails suspend(CustomerReasonCommand command);

}
