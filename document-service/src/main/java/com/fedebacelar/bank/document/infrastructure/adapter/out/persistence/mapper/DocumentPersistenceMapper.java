package com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.document.domain.model.Document;
import com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.entity.DocumentEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentPersistenceMapper {

    public DocumentEntity toEntity(Document document) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(document.id().toString());
        entity.setIdempotencyKey(document.idempotencyKey());
        entity.setContentSha256(document.contentSha256());
        entity.setBusinessContext(document.businessContext());
        entity.setBusinessReferenceId(document.businessReferenceId());
        entity.setCategory(document.category());
        entity.setOriginalFilename(document.originalFilename());
        entity.setContentType(document.contentType());
        entity.setSizeBytes(document.sizeBytes());
        entity.setStorageProvider(document.storageProvider());
        entity.setBucketName(document.bucketName());
        entity.setObjectKey(document.objectKey());
        entity.setStatus(document.status());
        entity.setCreatedAt(document.createdAt());
        entity.setUpdatedAt(document.updatedAt());
        entity.setVersion(document.version());
        return entity;
    }

    public Document toDomain(DocumentEntity entity) {
        return new Document(
                UUID.fromString(entity.getId()),
                entity.getIdempotencyKey(),
                entity.getContentSha256(),
                entity.getBusinessContext(),
                entity.getBusinessReferenceId(),
                entity.getCategory(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getStorageProvider(),
                entity.getBucketName(),
                entity.getObjectKey(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}

