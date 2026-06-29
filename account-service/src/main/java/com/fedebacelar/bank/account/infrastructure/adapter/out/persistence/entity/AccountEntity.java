package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity {

    @Id
    private String id;

    private String customerId;

    private String accountNumber;

    private String cbu;

    private String alias;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private Instant openedAt;

    private Instant closedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;
}
