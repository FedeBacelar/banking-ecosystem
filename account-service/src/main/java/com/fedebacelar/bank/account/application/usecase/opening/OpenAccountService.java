package com.fedebacelar.bank.account.application.usecase.opening;

import com.fedebacelar.bank.account.application.command.OpenAccountCommand;
import com.fedebacelar.bank.account.application.mapper.AccountDetailsMapper;
import com.fedebacelar.bank.account.application.port.in.OpenAccountUseCase;
import com.fedebacelar.bank.account.application.port.out.AccountAliasLookupPort;
import com.fedebacelar.bank.account.application.port.out.AccountNumberGeneratorPort;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.application.port.out.CbuGeneratorPort;
import com.fedebacelar.bank.account.application.port.out.CustomerLookupPort;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.exception.CustomerNotEligibleForAccountException;
import com.fedebacelar.bank.account.domain.exception.DuplicateAccountAliasException;
import com.fedebacelar.bank.account.domain.exception.ExternalCustomerNotFoundException;
import com.fedebacelar.bank.account.domain.model.Account;
import com.fedebacelar.bank.account.domain.model.AccountBalance;
import com.fedebacelar.bank.account.domain.model.AccountStatusHistory;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OpenAccountService implements OpenAccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountAliasLookupPort accountAliasLookupPort;
    private final AccountNumberGeneratorPort accountNumberGeneratorPort;
    private final CbuGeneratorPort cbuGeneratorPort;
    private final CustomerLookupPort customerLookupPort;
    private final Clock clock;

    public OpenAccountService(
            AccountRepositoryPort accountRepositoryPort,
            AccountAliasLookupPort accountAliasLookupPort,
            AccountNumberGeneratorPort accountNumberGeneratorPort,
            CbuGeneratorPort cbuGeneratorPort,
            CustomerLookupPort customerLookupPort,
            Clock clock
    ) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.accountAliasLookupPort = accountAliasLookupPort;
        this.accountNumberGeneratorPort = accountNumberGeneratorPort;
        this.cbuGeneratorPort = cbuGeneratorPort;
        this.customerLookupPort = customerLookupPort;
        this.clock = clock;
    }

    @Override
    public AccountDetails open(OpenAccountCommand command) {
        var customer = customerLookupPort.findCustomer(command.customerId())
                .orElseThrow(() -> new ExternalCustomerNotFoundException(command.customerId()));

        if (!customer.canOpenAccount()) {
            throw new CustomerNotEligibleForAccountException(command.customerId(), customer.status());
        }

        if (command.alias() != null && accountAliasLookupPort.existsAlias(command.alias())) {
            throw new DuplicateAccountAliasException(command.alias());
        }

        Instant now = Instant.now(clock);
        UUID accountId = UUID.randomUUID();
        String accountNumber = accountNumberGeneratorPort.nextAccountNumber();

        Account account = new Account(
                accountId,
                command.customerId(),
                accountNumber,
                cbuGeneratorPort.nextCbu(accountNumber),
                command.alias(),
                command.type(),
                command.currency(),
                AccountStatus.PENDING_ACTIVATION,
                now,
                null,
                now,
                now,
                null
        );

        AccountBalance balance = new AccountBalance(
                UUID.randomUUID(),
                accountId,
                command.currency(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                now,
                null
        );

        AccountStatusHistory history = new AccountStatusHistory(
                UUID.randomUUID(),
                accountId,
                null,
                AccountStatus.PENDING_ACTIVATION,
                "Account opened pending activation",
                "system",
                now
        );

        return AccountDetailsMapper.toDetails(accountRepositoryPort.save(new BankAccount(account, balance, List.of(history))));
    }
}
