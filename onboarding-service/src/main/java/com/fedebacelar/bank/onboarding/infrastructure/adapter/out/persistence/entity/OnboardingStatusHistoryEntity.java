package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "onboarding_status_history")
public class OnboardingStatusHistoryEntity {
    @Id @Column(nullable = false, updatable = false, length = 36)
    private String id;
    @Column(nullable = false, length = 36)
    private String applicationId;
    @Enumerated(EnumType.STRING) @Column(length = 40)
    private OnboardingApplicationStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private OnboardingApplicationStatus newStatus;
    @Column(nullable = false, length = 80)
    private String reasonCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private OnboardingActorType actorType;
    @Column(nullable = false)
    private Instant occurredAt;

    public OnboardingStatusHistoryEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public OnboardingApplicationStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(OnboardingApplicationStatus previousStatus) { this.previousStatus = previousStatus; }
    public OnboardingApplicationStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OnboardingApplicationStatus newStatus) { this.newStatus = newStatus; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public OnboardingActorType getActorType() { return actorType; }
    public void setActorType(OnboardingActorType actorType) { this.actorType = actorType; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
