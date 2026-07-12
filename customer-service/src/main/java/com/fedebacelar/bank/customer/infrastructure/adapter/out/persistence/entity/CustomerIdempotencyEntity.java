package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "customer_idempotency") @Getter @Setter @NoArgsConstructor
public class CustomerIdempotencyEntity {
    @Id @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;
    @Column(nullable = false, length = 64)
    private String requestHash;
    @Column(length = 36)
    private String resourceId;
    @Column(nullable = false)
    private Instant createdAt;
}
