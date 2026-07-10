package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "onboarding_work_item")
public class OnboardingWorkItemEntity {
    @Id @Column(nullable = false, updatable = false, length = 36)
    private String id;
    @Column(nullable = false, length = 36)
    private String applicationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private WorkflowJobType jobType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private WorkflowJobStatus status;
    @Column(nullable = false)
    private int attempts;
    @Column(nullable = false)
    private Instant nextAttemptAt;
    private Instant lockedUntil;
    @Column(length = 80)
    private String lastErrorCode;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    public OnboardingWorkItemEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public WorkflowJobType getJobType() { return jobType; }
    public void setJobType(WorkflowJobType jobType) { this.jobType = jobType; }
    public WorkflowJobStatus getStatus() { return status; }
    public void setStatus(WorkflowJobStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public String getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
