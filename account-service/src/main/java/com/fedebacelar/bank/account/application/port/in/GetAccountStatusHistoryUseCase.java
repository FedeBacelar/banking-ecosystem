package com.fedebacelar.bank.account.application.port.in;

import com.fedebacelar.bank.account.application.view.AccountStatusHistoryDetails;
import java.util.List;
import java.util.UUID;

public interface GetAccountStatusHistoryUseCase {

    List<AccountStatusHistoryDetails> getStatusHistory(UUID accountId);
}
