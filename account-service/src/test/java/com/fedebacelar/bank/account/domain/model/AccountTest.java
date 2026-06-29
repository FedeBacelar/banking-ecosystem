package com.fedebacelar.bank.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.exception.AccountClosedException;
import com.fedebacelar.bank.account.domain.exception.InvalidAccountStatusTransitionException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final Instant NOW = Instant.parse("2026-06-17T00:00:00Z");

    @Test
    void activatesPendingAccount() {
        Account account = account(AccountStatus.PENDING_ACTIVATION);

        Account activated = account.activate(NOW.plusSeconds(60));

        assertThat(activated.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(activated.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void doesNotFreezePendingAccount() {
        Account account = account(AccountStatus.PENDING_ACTIVATION);

        assertThatThrownBy(() -> account.freeze(NOW.plusSeconds(60)))
                .isInstanceOf(InvalidAccountStatusTransitionException.class);
    }

    @Test
    void doesNotChangeAliasOnClosedAccount() {
        Account account = account(AccountStatus.CLOSED);

        assertThatThrownBy(() -> account.withAlias("new.alias", NOW.plusSeconds(60)))
                .isInstanceOf(AccountClosedException.class);
    }

    private Account account(AccountStatus status) {
        return new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ACC-2026-000001",
                "2850001000020260000015",
                "fede.bank.ars",
                AccountType.SAVINGS,
                CurrencyCode.ARS,
                status,
                NOW,
                status == AccountStatus.CLOSED ? NOW : null,
                NOW,
                NOW,
                null
        );
    }
}
