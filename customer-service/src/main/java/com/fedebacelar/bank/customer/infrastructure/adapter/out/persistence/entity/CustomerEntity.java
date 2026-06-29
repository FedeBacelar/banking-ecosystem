package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity {

    @Id
    private String id;

    private String partyId;

    private String customerNumber;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    private LocalDate onboardingDate;

    private Instant closedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;
}
