package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_balance")
@Getter
@Setter
@NoArgsConstructor
public class AccountBalanceEntity {

    @Id
    private String id;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    private BigDecimal currentBalance;

    private BigDecimal availableBalance;

    private BigDecimal holdBalance;

    private Instant updatedAt;

    @Version
    private Long version;
}
