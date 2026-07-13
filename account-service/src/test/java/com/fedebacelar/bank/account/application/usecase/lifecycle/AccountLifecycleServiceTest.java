package com.fedebacelar.bank.account.application.usecase.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.account.application.command.AccountReasonCommand;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.model.Account;
import com.fedebacelar.bank.account.domain.model.AccountBalance;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class AccountLifecycleServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AccountReasonCommand ONBOARDING =
            new AccountReasonCommand("AUTO_ONBOARDING_APPROVED", "onboarding-service");

    private final AccountRepositoryPort accounts = org.mockito.Mockito.mock(AccountRepositoryPort.class);
    private final AccountLifecycleService service = new AccountLifecycleService(accounts, CLOCK);

    @Test
    void returnsAnAlreadyActiveAccountWithoutWritingDuplicateHistory() {
        BankAccount active = aggregate(AccountStatus.ACTIVE);
        when(accounts.findByAccountId(active.account().id())).thenReturn(Optional.of(active));

        var result = service.activate(active.account().id(), ONBOARDING);

        assertThat(result.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(accounts, never()).save(any());
    }

    @Test
    void activatesAPendingAccountAndRecordsTheTransition() {
        BankAccount pending = aggregate(AccountStatus.PENDING_ACTIVATION);
        when(accounts.findByAccountId(pending.account().id())).thenReturn(Optional.of(pending));
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.activate(pending.account().id(), ONBOARDING);

        assertThat(result.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(accounts).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.statusHistory().size() == 1
                        && saved.statusHistory().getFirst().previousStatus() == AccountStatus.PENDING_ACTIVATION
                        && saved.statusHistory().getFirst().newStatus() == AccountStatus.ACTIVE
        ));
    }

    @Test
    void treatsAConcurrentActivationAsSuccessWhenTheAccountIsAlreadyActive() {
        BankAccount pending = aggregate(AccountStatus.PENDING_ACTIVATION);
        BankAccount active = new BankAccount(
                pending.account().activate(NOW),
                pending.balance(),
                pending.statusHistory()
        );
        when(accounts.findByAccountId(pending.account().id()))
                .thenReturn(Optional.of(pending), Optional.of(active));
        when(accounts.save(any())).thenThrow(new ObjectOptimisticLockingFailureException(
                "Account", pending.account().id()
        ));

        var result = service.activate(pending.account().id(), ONBOARDING);

        assertThat(result.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(accounts).save(any());
    }

    private BankAccount aggregate(AccountStatus status) {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                "ACC-2026-000001",
                "2850001000020260000015",
                "fede.bank.ars",
                AccountType.SAVINGS,
                CurrencyCode.ARS,
                status,
                NOW.minusSeconds(60),
                null,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60),
                null
        );
        AccountBalance balance = new AccountBalance(
                UUID.randomUUID(), accountId, CurrencyCode.ARS,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, NOW, null
        );
        return new BankAccount(account, balance, List.of());
    }
}
