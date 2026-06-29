package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountBalanceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBalanceJpaRepository extends JpaRepository<AccountBalanceEntity, String> {

    Optional<AccountBalanceEntity> findByAccountId(String accountId);
}
