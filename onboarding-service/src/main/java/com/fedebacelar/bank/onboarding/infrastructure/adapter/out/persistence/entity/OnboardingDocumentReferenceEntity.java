package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(
        name = "onboarding_document_reference",
        uniqueConstraints = @UniqueConstraint(name = "uk_onboarding_document_reference_application_category", columnNames = {"application_id", "category"})
)
public class OnboardingDocumentReferenceEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OnboardingDocumentCategory category;

    @Column(nullable = false, length = 36)
    private String documentId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public OnboardingDocumentReferenceEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public OnboardingDocumentCategory getCategory() {
        return category;
    }

    public void setCategory(OnboardingDocumentCategory category) {
        this.category = category;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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
