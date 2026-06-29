package com.fedebacelar.bank.account.application.port.in;

import com.fedebacelar.bank.account.application.view.AccountBalanceDetails;
import java.util.UUID;

public interface GetAccountBalanceUseCase {

    AccountBalanceDetails getBalance(UUID accountId);
}
