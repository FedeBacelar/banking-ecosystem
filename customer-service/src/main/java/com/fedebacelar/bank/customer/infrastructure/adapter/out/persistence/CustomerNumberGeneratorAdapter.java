package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.customer.application.port.out.CustomerNumberGeneratorPort;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerNumberSequenceEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.CustomerNumberSequenceJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustomerNumberGeneratorAdapter implements CustomerNumberGeneratorPort {

    private static final long INITIAL_SEQUENCE_VALUE = 1L;

    private final CustomerNumberSequenceJpaRepository sequenceRepository;
    private final Clock clock;

    public CustomerNumberGeneratorAdapter(CustomerNumberSequenceJpaRepository sequenceRepository, Clock clock) {
        this.sequenceRepository = sequenceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String nextCustomerNumber() {
        int year = Year.now(clock).getValue();
        CustomerNumberSequenceEntity sequence = findOrCreateSequence(year);
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(Instant.now(clock));
        sequenceRepository.save(sequence);
        return "CUS-" + year + "-" + String.format("%06d", value);
    }

    private CustomerNumberSequenceEntity findOrCreateSequence(int year) {
        return sequenceRepository.findByYearForUpdate(year)
                .orElseGet(() -> createSequence(year));
    }

    private CustomerNumberSequenceEntity createSequence(int year) {
        try {
            CustomerNumberSequenceEntity sequence = new CustomerNumberSequenceEntity();
            sequence.setSequenceYear(year);
            sequence.setNextValue(INITIAL_SEQUENCE_VALUE);
            sequence.setUpdatedAt(Instant.now(clock));
            return sequenceRepository.saveAndFlush(sequence);
        } catch (DataIntegrityViolationException exception) {
            return sequenceRepository.findByYearForUpdate(year)
                    .orElseThrow(() -> exception);
        }
    }
}
