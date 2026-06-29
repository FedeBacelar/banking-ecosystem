package com.fedebacelar.bank.account.application.usecase.query;

import com.fedebacelar.bank.account.application.mapper.AccountDetailsMapper;
import com.fedebacelar.bank.account.application.port.in.GetAccountBalanceUseCase;
import com.fedebacelar.bank.account.application.port.in.GetAccountUseCase;
import com.fedebacelar.bank.account.application.port.in.GetCustomerAccountsUseCase;
import com.fedebacelar.bank.account.application.view.AccountBalanceDetails;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.domain.exception.AccountNotFoundException;
import com.fedebacelar.bank.account.domain.exception.AccountNumberNotFoundException;
import com.fedebacelar.bank.account.domain.exception.AliasNotFoundException;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetAccountService implements GetAccountUseCase, GetCustomerAccountsUseCase, GetAccountBalanceUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    public GetAccountService(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
    }

    @Override
    public AccountDetails getById(UUID accountId) {
        return accountRepositoryPort.findByAccountId(accountId)
                .map(AccountDetailsMapper::toDetails)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Override
    public AccountDetails getByNumber(String accountNumber) {
        return accountRepositoryPort.findByAccountNumber(accountNumber)
                .map(AccountDetailsMapper::toDetails)
                .orElseThrow(() -> new AccountNumberNotFoundException(accountNumber));
    }

    @Override
    public AccountDetails getByAlias(String alias) {
        return accountRepositoryPort.findByAlias(alias)
                .map(AccountDetailsMapper::toDetails)
                .orElseThrow(() -> new AliasNotFoundException(alias));
    }

    @Override
    public List<AccountDetails> getByCustomerId(UUID customerId) {
        return accountRepositoryPort.findByCustomerId(customerId).stream()
                .map(AccountDetailsMapper::toDetails)
                .toList();
    }

    @Override
    public AccountBalanceDetails getBalance(UUID accountId) {
        return accountRepositoryPort.findByAccountId(accountId)
                .map(BankAccount -> AccountDetailsMapper.toDetails(BankAccount.balance()))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
