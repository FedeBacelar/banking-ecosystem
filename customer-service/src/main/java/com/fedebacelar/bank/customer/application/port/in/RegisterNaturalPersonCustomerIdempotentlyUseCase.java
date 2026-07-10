package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.command.RegisterNaturalPersonCustomerCommand;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;

public interface RegisterNaturalPersonCustomerIdempotentlyUseCase {
    CustomerDetails register(String idempotencyKey, String requestHash, RegisterNaturalPersonCustomerCommand command);
}
