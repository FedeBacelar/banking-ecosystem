package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckOutcome;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "onboarding_review_check")
public class OnboardingReviewCheckEntity {
    @Id @Column(nullable = false, updatable = false, length = 36)
    private String id;
    @Column(nullable = false, length = 36)
    private String applicationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private ReviewCheckType checkType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ReviewCheckExecutionMode executionMode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ReviewCheckExecutionStatus executionStatus;
    @Enumerated(EnumType.STRING) @Column(length = 40)
    private ReviewCheckOutcome outcome;
    @Column(name = "blocking_check", nullable = false)
    private boolean blocking;
    @Column(nullable = false, length = 80)
    private String policyVersion;
    @Column(length = 80)
    private String provider;
    @Column(length = 80)
    private String reasonCode;
    @Column(nullable = false)
    private int attempts;
    private Instant startedAt;
    private Instant completedAt;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    public OnboardingReviewCheckEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public ReviewCheckType getCheckType() { return checkType; }
    public void setCheckType(ReviewCheckType checkType) { this.checkType = checkType; }
    public ReviewCheckExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(ReviewCheckExecutionMode executionMode) { this.executionMode = executionMode; }
    public ReviewCheckExecutionStatus getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(ReviewCheckExecutionStatus executionStatus) { this.executionStatus = executionStatus; }
    public ReviewCheckOutcome getOutcome() { return outcome; }
    public void setOutcome(ReviewCheckOutcome outcome) { this.outcome = outcome; }
    public boolean isBlocking() { return blocking; }
    public void setBlocking(boolean blocking) { this.blocking = blocking; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
