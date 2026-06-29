package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_number_sequence")
@Getter
@Setter
@NoArgsConstructor
public class AccountNumberSequenceEntity {

    @Id
    private Integer year;

    private Long nextValue;

    private Instant updatedAt;
}
