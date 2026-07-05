package com.fedebacelar.bank.document.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.view.DocumentDetails;
import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.exception.DocumentStorageException;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import com.fedebacelar.bank.document.infrastructure.adapter.in.web.dto.DocumentResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentWebMapper {

    public UploadDocumentCommand toCommand(
            String businessContext,
            String businessReferenceId,
            DocumentCategory category,
            MultipartFile file
    ) {
        return new UploadDocumentCommand(
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
        try {
            return new DocumentFile(
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    originalFilename(file)
            );
        } catch (IOException exception) {
            throw new DocumentStorageException("Could not read uploaded document file", exception);
        }
    }

    private String originalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "document";
        }
        return originalFilename;
    }
}
