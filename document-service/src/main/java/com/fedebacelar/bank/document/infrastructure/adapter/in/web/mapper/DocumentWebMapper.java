package com.fedebacelar.bank.document.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.view.DocumentDetails;
import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import com.fedebacelar.bank.document.infrastructure.adapter.in.web.dto.DocumentResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentWebMapper {

    public UploadDocumentCommand toCommand(
            String idempotencyKey,
            String contentSha256,
            String businessContext,
            String businessReferenceId,
            DocumentCategory category,
            MultipartFile file
    ) {
        return new UploadDocumentCommand(
                idempotencyKey,
                contentSha256,
                businessContext,
                businessReferenceId,
                category,
                toDocumentFile(file)
        );
    }

    public DocumentResponse toResponse(DocumentDetails details) {
        return new DocumentResponse(
                details.id(),
                details.businessContext(),
                details.businessReferenceId(),
                details.category(),
                details.originalFilename(),
                details.contentType(),
                details.sizeBytes(),
                details.storageProvider(),
                details.bucketName(),
                details.objectKey(),
                details.status(),
                details.createdAt(),
                details.updatedAt()
        );
    }

    private DocumentFile toDocumentFile(MultipartFile file) {
        return new DocumentFile(
                file::getInputStream,
                file.getSize(),
                file.getContentType(),
                originalFilename(file)
        );
    }

    private String originalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "document";
        }
        return originalFilename;
    }
}
