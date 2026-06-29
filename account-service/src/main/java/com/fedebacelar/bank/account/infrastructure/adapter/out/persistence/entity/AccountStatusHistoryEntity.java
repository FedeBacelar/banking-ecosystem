package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
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
@Table(name = "account_status_history")
@Getter
@Setter
@NoArgsConstructor
public class AccountStatusHistoryEntity {

    @Id
    private String id;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private AccountStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private AccountStatus newStatus;

    private String reason;

    private String changedBy;

    private Instant changedAt;
}
