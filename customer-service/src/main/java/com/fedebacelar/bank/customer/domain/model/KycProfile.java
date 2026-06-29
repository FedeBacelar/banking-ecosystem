package com.fedebacelar.bank.customer.domain.model;

import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import java.time.Instant;
import java.util.UUID;

public record KycProfile(
        UUID id,
        UUID customerId,
        RiskLevel riskLevel,
        KycStatus status,
        Instant lastReviewAt,
        Instant nextReviewAt
) {

    public KycProfile approve(Instant reviewedAt) {
        return new KycProfile(id, customerId, riskLevel, KycStatus.APPROVED, reviewedAt, nextReviewAt);
    }

    public KycProfile reject(Instant reviewedAt) {
        return new KycProfile(id, customerId, riskLevel, KycStatus.REJECTED, reviewedAt, nextReviewAt);
    }
}
