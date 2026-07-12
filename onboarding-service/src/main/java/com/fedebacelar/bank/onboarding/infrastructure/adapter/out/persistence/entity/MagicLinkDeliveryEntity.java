package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "onboarding_magic_link_delivery")
public class MagicLinkDeliveryEntity {
    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String applicationId;
    @Column(nullable = false, length = 36, unique = true)
    private String deliveryId;
    @Column(nullable = false, length = 255)
    private String recipient;
    @Column(columnDefinition = "TEXT")
    private String encryptedMagicLink;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant sentAt;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    public MagicLinkDeliveryEntity() {
    }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getEncryptedMagicLink() { return encryptedMagicLink; }
    public void setEncryptedMagicLink(String encryptedMagicLink) { this.encryptedMagicLink = encryptedMagicLink; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
