package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_number_sequence")
@Getter
@Setter
@NoArgsConstructor
public class CustomerNumberSequenceEntity {

    @Id
    private Integer sequenceYear;

    private Long nextValue;

    private Instant updatedAt;
}
