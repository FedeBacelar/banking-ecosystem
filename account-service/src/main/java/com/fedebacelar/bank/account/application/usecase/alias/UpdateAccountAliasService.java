package com.fedebacelar.bank.account.application.usecase.alias;

import com.fedebacelar.bank.account.application.command.AccountAliasCommand;
import com.fedebacelar.bank.account.application.mapper.AccountDetailsMapper;
import com.fedebacelar.bank.account.application.port.in.UpdateAccountAliasUseCase;
import com.fedebacelar.bank.account.application.port.out.AccountAliasLookupPort;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.domain.exception.AccountNotFoundException;
import com.fedebacelar.bank.account.domain.exception.DuplicateAccountAliasException;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UpdateAccountAliasService implements UpdateAccountAliasUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountAliasLookupPort accountAliasLookupPort;
    private final Clock clock;

    public UpdateAccountAliasService(
            AccountRepositoryPort accountRepositoryPort,
            AccountAliasLookupPort accountAliasLookupPort,
            Clock clock
    ) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.accountAliasLookupPort = accountAliasLookupPort;
        this.clock = clock;
    }

    @Override
    public AccountDetails updateAlias(UUID accountId, AccountAliasCommand command) {
        BankAccount aggregate = accountRepositoryPort.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (accountAliasLookupPort.existsAliasForOtherAccount(command.alias(), accountId)) {
            throw new DuplicateAccountAliasException(command.alias());
        }

        BankAccount updated = new BankAccount(
                aggregate.account().withAlias(command.alias(), Instant.now(clock)),
                aggregate.balance(),
                aggregate.statusHistory()
        );

        return AccountDetailsMapper.toDetails(accountRepositoryPort.save(updated));
    }
}
