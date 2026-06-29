package com.fedebacelar.bank.account.application.port.in;

import com.fedebacelar.bank.account.application.view.AccountDetails;
import java.util.List;
import java.util.UUID;

public interface GetCustomerAccountsUseCase {

    List<AccountDetails> getByCustomerId(UUID customerId);
}
