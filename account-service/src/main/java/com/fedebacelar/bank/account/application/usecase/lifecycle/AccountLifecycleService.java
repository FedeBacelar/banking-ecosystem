package com.fedebacelar.bank.account.application.usecase.lifecycle;

import com.fedebacelar.bank.account.application.command.AccountReasonCommand;
import com.fedebacelar.bank.account.application.mapper.AccountDetailsMapper;
import com.fedebacelar.bank.account.application.port.in.AccountLifecycleUseCase;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.exception.AccountBalanceNotZeroException;
import com.fedebacelar.bank.account.domain.exception.AccountNotFoundException;
import com.fedebacelar.bank.account.domain.model.Account;
import com.fedebacelar.bank.account.domain.model.AccountStatusHistory;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class AccountLifecycleService implements AccountLifecycleUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final Clock clock;

    public AccountLifecycleService(AccountRepositoryPort accountRepositoryPort, Clock clock) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.clock = clock;
    }

    @Override
    public AccountDetails activate(UUID accountId, AccountReasonCommand command) {
        BankAccount aggregate = accountRepositoryPort.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (aggregate.account().status() == AccountStatus.ACTIVE) {
            return AccountDetailsMapper.toDetails(aggregate);
        }
        try {
            return saveTransition(aggregate, aggregate.account().activate(Instant.now(clock)), command);
        } catch (ObjectOptimisticLockingFailureException concurrentActivation) {
            BankAccount current = accountRepositoryPort.findByAccountId(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));
            if (current.account().status() == AccountStatus.ACTIVE) {
                return AccountDetailsMapper.toDetails(current);
            }
            throw concurrentActivation;
        }
    }

    @Override
    public AccountDetails freeze(UUID accountId, AccountReasonCommand command) {
        return transition(accountId, command, account -> account.freeze(Instant.now(clock)));
    }

    @Override
    public AccountDetails close(UUID accountId, AccountReasonCommand command) {
        BankAccount aggregate = accountRepositoryPort.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!aggregate.balance().isZero()) {
            throw new AccountBalanceNotZeroException(accountId);
        }

        return saveTransition(aggregate, aggregate.account().close(Instant.now(clock)), command);
    }

    private AccountDetails transition(UUID accountId, AccountReasonCommand command, Function<Account, Account> transition) {
        BankAccount aggregate = accountRepositoryPort.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return saveTransition(aggregate, transition.apply(aggregate.account()), command);
    }

    private AccountDetails saveTransition(BankAccount aggregate, Account updatedAccount, AccountReasonCommand command) {
        Instant now = Instant.now(clock);
        var history = new ArrayList<>(aggregate.statusHistory());
        history.add(new AccountStatusHistory(
                UUID.randomUUID(),
                updatedAccount.id(),
                aggregate.account().status(),
                updatedAccount.status(),
                command.reason(),
                command.changedBy(),
                now
        ));

        return AccountDetailsMapper.toDetails(accountRepositoryPort.save(new BankAccount(updatedAccount, aggregate.balance(), history)));
    }
}
