package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "onboarding_application")
public class OnboardingApplicationEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OnboardingApplicationStatus status;

    @Column(nullable = false, length = 64)
    private String magicLinkTokenHash;

    @Column(nullable = false)
    private Instant magicLinkExpiresAt;

    private Instant magicLinkConsumedAt;

    private Instant emailVerifiedAt;

    @Column(length = 64)
    private String continuationTokenHash;

    private Instant continuationExpiresAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public OnboardingApplicationEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OnboardingApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingApplicationStatus status) {
        this.status = status;
    }

    public String getMagicLinkTokenHash() {
        return magicLinkTokenHash;
    }

    public void setMagicLinkTokenHash(String magicLinkTokenHash) {
        this.magicLinkTokenHash = magicLinkTokenHash;
    }

    public Instant getMagicLinkExpiresAt() {
        return magicLinkExpiresAt;
    }

    public void setMagicLinkExpiresAt(Instant magicLinkExpiresAt) {
        this.magicLinkExpiresAt = magicLinkExpiresAt;
    }

    public Instant getMagicLinkConsumedAt() {
        return magicLinkConsumedAt;
    }

    public void setMagicLinkConsumedAt(Instant magicLinkConsumedAt) {
        this.magicLinkConsumedAt = magicLinkConsumedAt;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public String getContinuationTokenHash() {
        return continuationTokenHash;
    }

    public void setContinuationTokenHash(String continuationTokenHash) {
        this.continuationTokenHash = continuationTokenHash;
    }

    public Instant getContinuationExpiresAt() {
        return continuationExpiresAt;
    }

    public void setContinuationExpiresAt(Instant continuationExpiresAt) {
        this.continuationExpiresAt = continuationExpiresAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
