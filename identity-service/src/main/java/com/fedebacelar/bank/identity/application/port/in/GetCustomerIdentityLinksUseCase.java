package com.fedebacelar.bank.identity.application.port.in;

import com.fedebacelar.bank.identity.application.view.IdentityLinkDetails;
import java.util.List;
import java.util.UUID;

public interface GetCustomerIdentityLinksUseCase {

    List<IdentityLinkDetails> getByCustomerId(UUID customerId);
}
