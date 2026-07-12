package com.fedebacelar.bank.account.application.usecase.opening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.account.TestcontainersConfiguration;
import com.fedebacelar.bank.account.application.command.OpenAccountCommand;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.application.port.out.CustomerLookupPort;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.enums.CustomerStatus;
import com.fedebacelar.bank.account.domain.model.CustomerRef;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IdempotentAccountOpeningIntegrationTest {

    @Autowired
    private IdempotentAccountOpeningService service;

    @Autowired
    private AccountRepositoryPort accounts;

    @MockitoBean
    private CustomerLookupPort customers;

    @Test
    void serializesConcurrentOpeningsUsingTheSameIdempotencyKey() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(customers.findCustomer(customerId)).thenReturn(Optional.of(
                new CustomerRef(customerId, "CUS-2026-000001", CustomerStatus.ACTIVE)
        ));
        OpenAccountCommand command = new OpenAccountCommand(
                customerId, AccountType.SAVINGS, CurrencyCode.ARS, null
        );
        String key = "onboarding:00000000-0000-0000-0000-000000000003:OPEN_ACCOUNT";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return service.open(key, "request-hash", command);
            });
            var second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return service.open(key, "request-hash", command);
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var firstResult = first.get(30, TimeUnit.SECONDS);
            var secondResult = second.get(30, TimeUnit.SECONDS);

            assertThat(secondResult.accountId()).isEqualTo(firstResult.accountId());
            assertThat(accounts.findByCustomerId(customerId))
                    .singleElement()
                    .satisfies(account -> assertThat(account.account().id()).isEqualTo(firstResult.accountId()));
        }
    }
}
