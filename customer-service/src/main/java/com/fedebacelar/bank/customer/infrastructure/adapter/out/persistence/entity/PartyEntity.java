package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.customer.domain.enums.PartyLifecycleStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyType;
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
@Table(name = "party")
@Getter
@Setter
@NoArgsConstructor
public class PartyEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private PartyType partyType;

    @Enumerated(EnumType.STRING)
    private PartyLifecycleStatus lifecycleStatus;

    private Instant createdAt;

    private Instant updatedAt;
}
