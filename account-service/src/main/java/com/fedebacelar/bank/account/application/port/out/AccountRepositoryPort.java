package com.fedebacelar.bank.account.application.port.out;

import com.fedebacelar.bank.account.domain.model.AccountStatusHistory;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {

    BankAccount save(BankAccount account);

    Optional<BankAccount> findByAccountId(UUID accountId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    Optional<BankAccount> findByAlias(String alias);

    List<BankAccount> findByCustomerId(UUID customerId);

    List<AccountStatusHistory> findStatusHistory(UUID accountId);
}
