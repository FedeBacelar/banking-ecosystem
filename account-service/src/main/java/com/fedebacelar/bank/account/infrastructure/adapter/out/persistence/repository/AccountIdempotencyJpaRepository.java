package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountIdempotencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountIdempotencyJpaRepository extends JpaRepository<AccountIdempotencyEntity, String> {
}
