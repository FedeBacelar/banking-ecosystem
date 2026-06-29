package com.fedebacelar.bank.account.application.port.in;

import com.fedebacelar.bank.account.application.view.AccountDetails;
import java.util.UUID;

public interface GetAccountUseCase {

    AccountDetails getById(UUID accountId);

    AccountDetails getByNumber(String accountNumber);

    AccountDetails getByAlias(String alias);
}
