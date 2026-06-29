package com.fedebacelar.bank.account.application.mapper;

import com.fedebacelar.bank.account.application.view.AccountStatusHistoryDetails;
import com.fedebacelar.bank.account.domain.model.AccountStatusHistory;

public final class AccountStatusHistoryMapper {

    private AccountStatusHistoryMapper() {
    }

    public static AccountStatusHistoryDetails toDetails(AccountStatusHistory history) {
        return new AccountStatusHistoryDetails(
                history.accountId(),
                history.previousStatus(),
                history.newStatus(),
                history.reason(),
                history.changedBy(),
                history.changedAt()
        );
    }
}
