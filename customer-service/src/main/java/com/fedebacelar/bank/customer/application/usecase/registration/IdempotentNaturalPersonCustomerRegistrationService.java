package com.fedebacelar.bank.customer.application.usecase.registration;

import com.fedebacelar.bank.customer.application.command.RegisterNaturalPersonCustomerCommand;
import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.model.IdempotencyRecord;
import com.fedebacelar.bank.customer.application.port.in.RegisterNaturalPersonCustomerIdempotentlyUseCase;
import com.fedebacelar.bank.customer.application.port.in.RegisterNaturalPersonCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerIdempotencyRepositoryPort;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.exception.IdempotencyConflictException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IdempotentNaturalPersonCustomerRegistrationService implements RegisterNaturalPersonCustomerIdempotentlyUseCase {
    private final RegisterNaturalPersonCustomerUseCase registration;
    private final CustomerIdempotencyRepositoryPort idempotencyRepository;
    private final CustomerRepositoryPort customerRepository;
    private final Clock clock;

    public IdempotentNaturalPersonCustomerRegistrationService(RegisterNaturalPersonCustomerUseCase registration,
            CustomerIdempotencyRepositoryPort idempotencyRepository, CustomerRepositoryPort customerRepository, Clock clock) {
        this.registration = registration;
        this.idempotencyRepository = idempotencyRepository;
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CustomerDetails register(String key, String requestHash, RegisterNaturalPersonCustomerCommand command) {
        if (!StringUtils.hasText(key)) {
            return registration.register(command);
        }
        return idempotencyRepository.findByKey(key)
                .map(record -> recover(record, requestHash))
                .orElseGet(() -> create(key, requestHash, command));
    }

    private CustomerDetails recover(IdempotencyRecord record, String requestHash) {
        if (!record.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException();
        }
        return customerRepository.findByCustomerId(record.resourceId())
                .map(CustomerDetailsMapper::toDetails)
                .orElseThrow(() -> new IllegalStateException("Idempotency record references a missing customer."));
    }

    private CustomerDetails create(String key, String requestHash, RegisterNaturalPersonCustomerCommand command) {
        CustomerDetails created = registration.register(command);
        idempotencyRepository.save(new IdempotencyRecord(key, requestHash, created.customerId(), Instant.now(clock)));
        return created;
    }
}
