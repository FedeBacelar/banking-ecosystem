package com.fedebacelar.bank.account.application.usecase.opening;

import com.fedebacelar.bank.account.application.command.OpenAccountCommand;
import com.fedebacelar.bank.account.application.mapper.AccountDetailsMapper;
import com.fedebacelar.bank.account.application.model.IdempotencyRecord;
import com.fedebacelar.bank.account.application.port.in.OpenAccountIdempotentlyUseCase;
import com.fedebacelar.bank.account.application.port.in.OpenAccountUseCase;
import com.fedebacelar.bank.account.application.port.out.AccountIdempotencyRepositoryPort;
import com.fedebacelar.bank.account.application.port.out.AccountRepositoryPort;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.domain.exception.IdempotencyConflictException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IdempotentAccountOpeningService implements OpenAccountIdempotentlyUseCase {
    private final OpenAccountUseCase opening;
    private final AccountIdempotencyRepositoryPort idempotencyRepository;
    private final AccountRepositoryPort accountRepository;
    private final Clock clock;

    public IdempotentAccountOpeningService(OpenAccountUseCase opening, AccountIdempotencyRepositoryPort idempotencyRepository,
            AccountRepositoryPort accountRepository, Clock clock) {
        this.opening = opening;
        this.idempotencyRepository = idempotencyRepository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountDetails open(String key, String requestHash, OpenAccountCommand command) {
        if (!StringUtils.hasText(key)) {
            return opening.open(command);
        }
        return idempotencyRepository.findByKey(key)
                .map(record -> recover(record, requestHash))
                .orElseGet(() -> create(key, requestHash, command));
    }

    private AccountDetails recover(IdempotencyRecord record, String requestHash) {
        if (!record.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException();
        }
        return accountRepository.findByAccountId(record.resourceId())
                .map(AccountDetailsMapper::toDetails)
                .orElseThrow(() -> new IllegalStateException("Idempotency record references a missing account."));
    }

    private AccountDetails create(String key, String requestHash, OpenAccountCommand command) {
        AccountDetails created = opening.open(command);
        idempotencyRepository.save(new IdempotencyRecord(key, requestHash, created.accountId(), Instant.now(clock)));
        return created;
    }
}
