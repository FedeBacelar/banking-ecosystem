package com.fedebacelar.bank.document.application.mapper;

import com.fedebacelar.bank.document.application.view.DocumentDetails;
import com.fedebacelar.bank.document.domain.model.Document;

public final class DocumentDetailsMapper {

    private DocumentDetailsMapper() {
    }

    public static DocumentDetails toDetails(Document document) {
        return new DocumentDetails(
                document.id(),
                document.contentSha256(),
                document.businessContext(),
                document.businessReferenceId(),
                document.category(),
                document.originalFilename(),
                document.contentType(),
                document.sizeBytes(),
                document.storageProvider(),
                document.bucketName(),
                document.objectKey(),
                document.status(),
                document.createdAt(),
                document.updatedAt()
        );
    }
}

