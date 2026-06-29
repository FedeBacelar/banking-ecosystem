package com.fedebacelar.bank.account.domain.model;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import com.fedebacelar.bank.account.domain.exception.AccountClosedException;
import com.fedebacelar.bank.account.domain.exception.InvalidAccountStatusTransitionException;
import java.time.Instant;
import java.util.UUID;

public record Account(
        UUID id,
        UUID customerId,
        String accountNumber,
        String cbu,
        String alias,
        AccountType type,
        CurrencyCode currency,
        AccountStatus status,
        Instant openedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {

    public Account withAlias(String newAlias, Instant changedAt) {
        ensureNotClosed("change alias");
        return new Account(id, customerId, accountNumber, cbu, newAlias, type, currency, status, openedAt, closedAt, createdAt, changedAt, version);
    }

    public Account activate(Instant changedAt) {
        if (status != AccountStatus.PENDING_ACTIVATION && status != AccountStatus.FROZEN) {
            throw new InvalidAccountStatusTransitionException(id, status, AccountStatus.ACTIVE);
        }
        return withStatus(AccountStatus.ACTIVE, changedAt);
    }

    public Account freeze(Instant changedAt) {
        if (status != AccountStatus.ACTIVE) {
            throw new InvalidAccountStatusTransitionException(id, status, AccountStatus.FROZEN);
        }
        return withStatus(AccountStatus.FROZEN, changedAt);
    }

    public Account close(Instant changedAt) {
        if (status == AccountStatus.CLOSED) {
            throw new InvalidAccountStatusTransitionException(id, status, AccountStatus.CLOSED);
        }
        return withStatus(AccountStatus.CLOSED, changedAt);
    }

    public void ensureNotClosed(String operation) {
        if (status == AccountStatus.CLOSED) {
            throw new AccountClosedException(id, operation);
        }
    }

    private Account withStatus(AccountStatus newStatus, Instant changedAt) {
        Instant closedAtValue = newStatus == AccountStatus.CLOSED ? changedAt : closedAt;
        return new Account(id, customerId, accountNumber, cbu, alias, type, currency, newStatus, openedAt, closedAtValue, createdAt, changedAt, version);
    }
}
