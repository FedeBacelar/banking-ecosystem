package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.account.TestcontainersConfiguration;
import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.model.Account;
import com.fedebacelar.bank.account.domain.model.AccountBalance;
import com.fedebacelar.bank.account.domain.model.AccountStatusHistory;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest
@Import({TestcontainersConfiguration.class, AccountPersistenceAdapterIntegrationTest.FixedClockConfig.class})
class AccountPersistenceAdapterIntegrationTest {

    @Autowired
    private AccountPersistenceAdapter adapter;

    @Autowired
    private AccountNumberGeneratorAdapter accountNumberGeneratorAdapter;

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-06-17T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void savesAndFindsAccountAggregate() {
        BankAccount account = aggregate();

        adapter.save(account);

        var found = adapter.findByAccountNumber("ACC-2026-000001");

        assertThat(found).isPresent();
        assertThat(found.get().account().status()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(found.get().balance().currentBalance()).isEqualByComparingTo("0.00");
        assertThat(adapter.findByAlias("fede.bank.ars")).isPresent();
        assertThat(adapter.findByCustomerId(account.account().customerId())).hasSize(1);
        assertThat(adapter.findStatusHistory(account.account().id())).hasSize(1);
    }

    @Test
    void generatesAccountNumbersFromTransactionalSequence() {
        assertThat(accountNumberGeneratorAdapter.nextAccountNumber()).isEqualTo("ACC-2026-000001");
        assertThat(accountNumberGeneratorAdapter.nextAccountNumber()).isEqualTo("ACC-2026-000002");
    }

    private BankAccount aggregate() {
        Instant now = Instant.parse("2026-06-17T00:00:00Z");
        UUID accountId = UUID.randomUUID();
        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                "ACC-2026-000001",
                "2850001000020260000015",
                "fede.bank.ars",
                AccountType.SAVINGS,
                CurrencyCode.ARS,
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
                CurrencyCode.ARS,
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
        return new BankAccount(account, balance, List.of(history));
    }
}
