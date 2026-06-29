package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, String> {

    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    Optional<AccountEntity> findByAlias(String alias);

    List<AccountEntity> findByCustomerIdOrderByOpenedAtDesc(String customerId);

    boolean existsByAlias(String alias);

    boolean existsByAliasAndIdNot(String alias, String id);
}
