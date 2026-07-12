package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountIdempotencyEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountIdempotencyJpaRepository extends JpaRepository<AccountIdempotencyEntity, String> {
    @Modifying
    @Query(value = """
            INSERT INTO account_idempotency (idempotency_key, request_hash, resource_id, created_at)
            VALUES (:key, :requestHash, NULL, :createdAt)
            ON DUPLICATE KEY UPDATE idempotency_key = idempotency_key
            """, nativeQuery = true)
    int insertIfAbsent(@Param("key") String key, @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from AccountIdempotencyEntity record where record.idempotencyKey = :key")
    Optional<AccountIdempotencyEntity> findByKeyForUpdate(@Param("key") String key);
}
