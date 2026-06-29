package com.fedebacelar.bank.account.application.usecase.opening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.account.application.command.OpenAccountCommand;
import com.fedebacelar.bank.account.application.port.out.AccountAliasLookupPort;
import com.fedebacelar.bank.account.application.port.out.AccountNumberGeneratorPort;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.application.port.out.CbuGeneratorPort;
import com.fedebacelar.bank.account.application.port.out.CustomerLookupPort;
import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.enums.CustomerStatus;
import com.fedebacelar.bank.account.domain.exception.CustomerNotEligibleForAccountException;
import com.fedebacelar.bank.account.domain.exception.DuplicateAccountAliasException;
import com.fedebacelar.bank.account.domain.model.BankAccount;
import com.fedebacelar.bank.account.domain.model.CustomerRef;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-17T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private AccountAliasLookupPort accountAliasLookupPort;

    @Mock
    private AccountNumberGeneratorPort accountNumberGeneratorPort;

    @Mock
    private CbuGeneratorPort cbuGeneratorPort;

    @Mock
    private CustomerLookupPort customerLookupPort;

    @Test
    void opensAccountForActiveCustomer() {
        UUID customerId = UUID.randomUUID();
        OpenAccountCommand command = new OpenAccountCommand(customerId, AccountType.SAVINGS, CurrencyCode.ARS, "fede.bank.ars");
        when(customerLookupPort.findCustomer(customerId)).thenReturn(Optional.of(new CustomerRef(customerId, "CUS-2026-000001", CustomerStatus.ACTIVE)));
        when(accountAliasLookupPort.existsAlias("fede.bank.ars")).thenReturn(false);
        when(accountNumberGeneratorPort.nextAccountNumber()).thenReturn("ACC-2026-000001");
        when(cbuGeneratorPort.nextCbu("ACC-2026-000001")).thenReturn("2850001000020260000015");
        when(accountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = service();

        var details = service.open(command);

        assertThat(details.customerId()).isEqualTo(customerId);
        assertThat(details.status()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(details.balance().currentBalance()).isZero();
        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(accountRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().statusHistory()).hasSize(1);
    }

    @Test
    void rejectsInactiveCustomer() {
        UUID customerId = UUID.randomUUID();
        OpenAccountCommand command = new OpenAccountCommand(customerId, AccountType.SAVINGS, CurrencyCode.ARS, null);
        when(customerLookupPort.findCustomer(customerId)).thenReturn(Optional.of(new CustomerRef(customerId, "CUS-2026-000001", CustomerStatus.PENDING_KYC)));

        assertThatThrownBy(() -> service().open(command))
                .isInstanceOf(CustomerNotEligibleForAccountException.class);
    }

    @Test
    void rejectsDuplicateAlias() {
        UUID customerId = UUID.randomUUID();
        OpenAccountCommand command = new OpenAccountCommand(customerId, AccountType.SAVINGS, CurrencyCode.ARS, "fede.bank.ars");
        when(customerLookupPort.findCustomer(customerId)).thenReturn(Optional.of(new CustomerRef(customerId, "CUS-2026-000001", CustomerStatus.ACTIVE)));
        when(accountAliasLookupPort.existsAlias("fede.bank.ars")).thenReturn(true);

        assertThatThrownBy(() -> service().open(command))
                .isInstanceOf(DuplicateAccountAliasException.class);
    }

    private OpenAccountService service() {
        return new OpenAccountService(
                accountRepositoryPort,
                accountAliasLookupPort,
                accountNumberGeneratorPort,
                cbuGeneratorPort,
                customerLookupPort,
                CLOCK
        );
    }
}
