package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.account.domain.model.Account;
import com.fedebacelar.bank.account.domain.model.AccountBalance;
import com.fedebacelar.bank.account.domain.model.AccountStatusHistory;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountBalanceEntity;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountStatusHistoryEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceMapper {

    public AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.setId(account.id().toString());
        entity.setCustomerId(account.customerId().toString());
        entity.setAccountNumber(account.accountNumber());
        entity.setCbu(account.cbu());
        entity.setAlias(account.alias());
        entity.setType(account.type());
        entity.setCurrency(account.currency());
        entity.setStatus(account.status());
        entity.setOpenedAt(account.openedAt());
        entity.setClosedAt(account.closedAt());
        entity.setCreatedAt(account.createdAt());
        entity.setUpdatedAt(account.updatedAt());
        entity.setVersion(account.version());
        return entity;
    }

    public Account toDomain(AccountEntity entity) {
        return new Account(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getCustomerId()),
                entity.getAccountNumber(),
                entity.getCbu(),
                entity.getAlias(),
                entity.getType(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getOpenedAt(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public AccountBalanceEntity toEntity(AccountBalance balance) {
        AccountBalanceEntity entity = new AccountBalanceEntity();
        entity.setId(balance.id().toString());
        entity.setAccountId(balance.accountId().toString());
        entity.setCurrency(balance.currency());
        entity.setCurrentBalance(balance.currentBalance());
        entity.setAvailableBalance(balance.availableBalance());
        entity.setHoldBalance(balance.holdBalance());
        entity.setUpdatedAt(balance.updatedAt());
        entity.setVersion(balance.version());
        return entity;
    }

    public AccountBalance toDomain(AccountBalanceEntity entity) {
        return new AccountBalance(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getAccountId()),
                entity.getCurrency(),
                entity.getCurrentBalance(),
                entity.getAvailableBalance(),
                entity.getHoldBalance(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public AccountStatusHistoryEntity toEntity(AccountStatusHistory history) {
        AccountStatusHistoryEntity entity = new AccountStatusHistoryEntity();
        entity.setId(history.id().toString());
        entity.setAccountId(history.accountId().toString());
        entity.setPreviousStatus(history.previousStatus());
        entity.setNewStatus(history.newStatus());
        entity.setReason(history.reason());
        entity.setChangedBy(history.changedBy());
        entity.setChangedAt(history.changedAt());
        return entity;
    }

    public AccountStatusHistory toDomain(AccountStatusHistoryEntity entity) {
        return new AccountStatusHistory(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getAccountId()),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getReason(),
                entity.getChangedBy(),
                entity.getChangedAt()
        );
    }
}
