package com.fedebacelar.bank.account.application.usecase.query;

import com.fedebacelar.bank.account.application.mapper.AccountStatusHistoryMapper;
import com.fedebacelar.bank.account.application.port.in.GetAccountStatusHistoryUseCase;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.application.view.AccountStatusHistoryDetails;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetAccountStatusHistoryService implements GetAccountStatusHistoryUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    public GetAccountStatusHistoryService(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
    }

    @Override
    public List<AccountStatusHistoryDetails> getStatusHistory(UUID accountId) {
        return accountRepositoryPort.findStatusHistory(accountId).stream()
                .map(AccountStatusHistoryMapper::toDetails)
                .toList();
    }
}
