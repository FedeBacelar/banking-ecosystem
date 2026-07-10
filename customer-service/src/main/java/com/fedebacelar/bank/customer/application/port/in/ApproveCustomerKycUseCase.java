package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import java.util.UUID;

public interface ApproveCustomerKycUseCase {

    CustomerDetails approveKyc(UUID customerId, String reasonCode, String changedBy);

    default CustomerDetails approveKyc(UUID customerId) {
        return approveKyc(customerId, "KYC_APPROVED", "system");
    }

}
