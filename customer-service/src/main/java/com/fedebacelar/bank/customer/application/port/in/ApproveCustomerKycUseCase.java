package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import java.util.UUID;

public interface ApproveCustomerKycUseCase {

    CustomerDetails approveKyc(UUID customerId);

}
