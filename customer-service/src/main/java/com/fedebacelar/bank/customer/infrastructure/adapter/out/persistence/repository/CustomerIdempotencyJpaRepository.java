package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerIdempotencyEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerIdempotencyJpaRepository extends JpaRepository<CustomerIdempotencyEntity, String> {
    @Modifying
    @Query(value = """
            INSERT INTO customer_idempotency (idempotency_key, request_hash, resource_id, created_at)
            VALUES (:key, :requestHash, NULL, :createdAt)
            ON DUPLICATE KEY UPDATE idempotency_key = idempotency_key
            """, nativeQuery = true)
    int insertIfAbsent(@Param("key") String key, @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from CustomerIdempotencyEntity record where record.idempotencyKey = :key")
    Optional<CustomerIdempotencyEntity> findByKeyForUpdate(@Param("key") String key);
}
