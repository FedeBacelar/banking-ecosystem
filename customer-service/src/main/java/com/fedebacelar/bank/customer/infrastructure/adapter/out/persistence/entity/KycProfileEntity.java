package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
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
@Table(name = "kyc_profile")
@Getter
@Setter
@NoArgsConstructor
public class KycProfileEntity {

    @Id
    private String id;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    private Instant lastReviewAt;

    private Instant nextReviewAt;
}
