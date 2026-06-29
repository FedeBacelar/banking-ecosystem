package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_status_history")
@Getter
@Setter
@NoArgsConstructor
public class CustomerStatusHistoryEntity {

    @Id
    private String id;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private CustomerStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private CustomerStatus newStatus;

    private String reason;

    private Instant changedAt;
}
