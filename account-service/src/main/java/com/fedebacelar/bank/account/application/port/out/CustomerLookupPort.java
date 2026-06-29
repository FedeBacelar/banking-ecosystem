package com.fedebacelar.bank.account.application.port.out;

import com.fedebacelar.bank.account.domain.model.CustomerRef;
import java.util.Optional;
import java.util.UUID;

public interface CustomerLookupPort {

    Optional<CustomerRef> findCustomer(UUID customerId);
}
