package com.fedebacelar.bank.account.application.usecase.opening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.account.application.command.OpenAccountCommand;
import com.fedebacelar.bank.account.application.mapper.AccountDetailsMapper;
import com.fedebacelar.bank.account.application.model.IdempotencyRecord;
import com.fedebacelar.bank.account.application.port.in.OpenAccountUseCase;
import com.fedebacelar.bank.account.application.port.out.AccountIdempotencyRepositoryPort;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.exception.IdempotencyConflictException;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IdempotentAccountOpeningServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void returnsSameAccountForRepeatedKeyAndRejectsPayloadDrift() {
        OpenAccountUseCase opening = mock(OpenAccountUseCase.class);
        AccountIdempotencyRepositoryPort idempotency = mock(AccountIdempotencyRepositoryPort.class);
        AccountRepositoryPort accounts = mock(AccountRepositoryPort.class);
        AtomicReference<IdempotencyRecord> record = new AtomicReference<>();
        BankAccount bankAccount = bankAccount();
        OpenAccountCommand command = new OpenAccountCommand(
                bankAccount.account().customerId(), AccountType.SAVINGS, CurrencyCode.ARS, null
        );
        when(opening.open(command)).thenReturn(AccountDetailsMapper.toDetails(bankAccount));
        when(idempotency.acquire(org.mockito.ArgumentMatchers.eq("onboarding:application:OPEN_ACCOUNT"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    IdempotencyRecord existing = record.get();
                    if (existing != null) {
                        return existing;
                    }
                    IdempotencyRecord pending = IdempotencyRecord.pending(
                            invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)
                    );
                    record.compareAndSet(null, pending);
                    return record.get();
                });
        when(idempotency.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            IdempotencyRecord saved = invocation.getArgument(0);
            record.set(saved);
            return saved;
        });
        when(accounts.findByAccountId(bankAccount.account().id())).thenReturn(Optional.of(bankAccount));
        IdempotentAccountOpeningService service = new IdempotentAccountOpeningService(
                opening, idempotency, accounts, Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var first = service.open("onboarding:application:OPEN_ACCOUNT", "hash-a", command);
        var repeated = service.open("onboarding:application:OPEN_ACCOUNT", "hash-a", command);

        assertThat(repeated.accountId()).isEqualTo(first.accountId());
        verify(opening, times(1)).open(command);
        assertThatThrownBy(() -> service.open("onboarding:application:OPEN_ACCOUNT", "hash-b", command))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    private BankAccount bankAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, UUID.randomUUID(), "ACC-2026-000001",
                "2850001000020260000015", null, AccountType.SAVINGS, CurrencyCode.ARS,
                AccountStatus.PENDING_ACTIVATION, NOW, null, NOW, NOW, 0L);
        AccountBalance balance = new AccountBalance(UUID.randomUUID(), accountId, CurrencyCode.ARS,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, NOW, 0L);
        return new BankAccount(account, balance, List.of());
    }
}
