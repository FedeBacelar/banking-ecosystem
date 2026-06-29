package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.account.application.port.out.AccountAliasLookupPort;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.domain.model.AccountStatusHistory;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.mapper.AccountPersistenceMapper;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository.AccountBalanceJpaRepository;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository.AccountJpaRepository;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository.AccountStatusHistoryJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountPersistenceAdapter implements AccountRepositoryPort, AccountAliasLookupPort {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountBalanceJpaRepository accountBalanceJpaRepository;
    private final AccountStatusHistoryJpaRepository accountStatusHistoryJpaRepository;
    private final AccountPersistenceMapper mapper;

    public AccountPersistenceAdapter(
            AccountJpaRepository accountJpaRepository,
            AccountBalanceJpaRepository accountBalanceJpaRepository,
            AccountStatusHistoryJpaRepository accountStatusHistoryJpaRepository,
            AccountPersistenceMapper mapper
    ) {
        this.accountJpaRepository = accountJpaRepository;
        this.accountBalanceJpaRepository = accountBalanceJpaRepository;
        this.accountStatusHistoryJpaRepository = accountStatusHistoryJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public BankAccount save(BankAccount account) {
        AccountEntity accountEntity = accountJpaRepository.save(mapper.toEntity(account.account()));
        accountBalanceJpaRepository.save(mapper.toEntity(account.balance()));
        accountStatusHistoryJpaRepository.saveAll(account.statusHistory().stream().map(mapper::toEntity).toList());
        return assemble(accountEntity)
                .orElseThrow(() -> new IllegalStateException("Saved account aggregate could not be reassembled: " + accountEntity.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankAccount> findByAccountId(UUID accountId) {
        return accountJpaRepository.findById(accountId.toString()).flatMap(this::assemble);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return accountJpaRepository.findByAccountNumber(accountNumber).flatMap(this::assemble);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankAccount> findByAlias(String alias) {
        return accountJpaRepository.findByAlias(alias).flatMap(this::assemble);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> findByCustomerId(UUID customerId) {
        return accountJpaRepository.findByCustomerIdOrderByOpenedAtDesc(customerId.toString()).stream()
                .map(this::assemble)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountStatusHistory> findStatusHistory(UUID accountId) {
        return accountStatusHistoryJpaRepository.findByAccountIdOrderByChangedAtAsc(accountId.toString()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAlias(String alias) {
        return accountJpaRepository.existsByAlias(alias);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAliasForOtherAccount(String alias, UUID accountId) {
        return accountJpaRepository.existsByAliasAndIdNot(alias, accountId.toString());
    }

    private Optional<BankAccount> assemble(AccountEntity accountEntity) {
        String accountId = accountEntity.getId();
        var balance = accountBalanceJpaRepository.findByAccountId(accountId);

        if (balance.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new BankAccount(
                mapper.toDomain(accountEntity),
                mapper.toDomain(balance.get()),
                accountStatusHistoryJpaRepository.findByAccountIdOrderByChangedAtAsc(accountId).stream()
                        .map(mapper::toDomain)
                        .toList()
        ));
    }
}
