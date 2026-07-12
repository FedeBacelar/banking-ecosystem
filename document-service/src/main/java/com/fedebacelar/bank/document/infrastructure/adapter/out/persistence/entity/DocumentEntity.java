package com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.enums.DocumentStorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "document")
public class DocumentEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(unique = true, length = 200)
    private String idempotencyKey;

    @Column(length = 64)
    private String contentSha256;

    @Column(nullable = false, length = 80)
    private String businessContext;

    @Column(nullable = false, length = 120)
    private String businessReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentCategory category;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentStorageProvider storageProvider;

    @Column(nullable = false, length = 120)
    private String bucketName;

    @Column(nullable = false, length = 500)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public DocumentEntity() {
    }

    public String getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public void setContentSha256(String contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusinessContext() {
        return businessContext;
    }

    public void setBusinessContext(String businessContext) {
        this.businessContext = businessContext;
    }

    public String getBusinessReferenceId() {
        return businessReferenceId;
    }

    public void setBusinessReferenceId(String businessReferenceId) {
        this.businessReferenceId = businessReferenceId;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public DocumentStorageProvider getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(DocumentStorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
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

