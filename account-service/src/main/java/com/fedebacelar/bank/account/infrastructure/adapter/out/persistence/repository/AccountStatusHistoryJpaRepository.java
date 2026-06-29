package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountStatusHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatusHistoryJpaRepository extends JpaRepository<AccountStatusHistoryEntity, String> {

    List<AccountStatusHistoryEntity> findByAccountIdOrderByChangedAtAsc(String accountId);
}
