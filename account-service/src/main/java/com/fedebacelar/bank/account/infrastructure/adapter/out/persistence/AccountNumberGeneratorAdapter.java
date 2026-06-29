package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.account.application.port.out.AccountNumberGeneratorPort;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountNumberSequenceEntity;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository.AccountNumberSequenceJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountNumberGeneratorAdapter implements AccountNumberGeneratorPort {

    private final AccountNumberSequenceJpaRepository accountNumberSequenceJpaRepository;
    private final Clock clock;

    public AccountNumberGeneratorAdapter(AccountNumberSequenceJpaRepository accountNumberSequenceJpaRepository, Clock clock) {
        this.accountNumberSequenceJpaRepository = accountNumberSequenceJpaRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String nextAccountNumber() {
        int year = Year.now(clock).getValue();
        AccountNumberSequenceEntity sequence = accountNumberSequenceJpaRepository.findByYearForUpdate(year)
                .orElseGet(() -> newSequence(year));

        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(Instant.now(clock));
        accountNumberSequenceJpaRepository.save(sequence);

        return "ACC-" + year + "-" + String.format("%06d", value);
    }

    private AccountNumberSequenceEntity newSequence(int year) {
        AccountNumberSequenceEntity sequence = new AccountNumberSequenceEntity();
        sequence.setYear(year);
        sequence.setNextValue(1L);
        sequence.setUpdatedAt(Instant.now(clock));
        return sequence;
    }
}
