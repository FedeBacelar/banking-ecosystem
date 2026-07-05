package com.fedebacelar.bank.document.application.usecase;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.mapper.DocumentDetailsMapper;
import com.fedebacelar.bank.document.application.port.in.GetDocumentUseCase;
import com.fedebacelar.bank.document.application.port.in.UploadDocumentUseCase;
import com.fedebacelar.bank.document.application.port.out.DocumentRepositoryPort;
import com.fedebacelar.bank.document.application.port.out.ObjectStoragePort;
import com.fedebacelar.bank.document.application.view.DocumentDetails;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.enums.DocumentStorageProvider;
import com.fedebacelar.bank.document.domain.exception.DocumentNotFoundException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentContentTypeException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentSizeException;
import com.fedebacelar.bank.document.domain.model.Document;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService implements UploadDocumentUseCase, GetDocumentUseCase {

    private final DocumentRepositoryPort repositoryPort;
    private final ObjectStoragePort objectStoragePort;
    private final Clock clock;
    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public DocumentService(
            DocumentRepositoryPort repositoryPort,
            ObjectStoragePort objectStoragePort,
            Clock clock,
            @Value("${document.upload.max-size-bytes:10485760}") long maxSizeBytes,
            @Value("${document.upload.allowed-content-types:image/jpeg,image/png,application/pdf}") Set<String> allowedContentTypes
    ) {
        this.repositoryPort = repositoryPort;
        this.objectStoragePort = objectStoragePort;
        this.clock = clock;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedContentTypes = allowedContentTypes;
    }

    @Override
    @Transactional
    public DocumentDetails upload(UploadDocumentCommand command) {
        validateFile(command.file());

        UUID documentId = UUID.randomUUID();
        String objectKey = objectKey(command, documentId);
        ObjectStoragePort.StoredObject storedObject = objectStoragePort.store(objectKey, command.file());
        Instant now = Instant.now(clock);

        Document document = new Document(
                documentId,
                command.businessContext(),
                command.businessReferenceId(),
                command.category(),
                command.file().originalFilename(),
                command.file().contentType(),
                command.file().size(),
                DocumentStorageProvider.MINIO,
                storedObject.bucketName(),
                storedObject.objectKey(),
                DocumentStatus.STORED,
                now,
                now,
                0L
        );

        return DocumentDetailsMapper.toDetails(repositoryPort.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDetails get(UUID documentId) {
        Document document = repositoryPort.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return DocumentDetailsMapper.toDetails(document);
    }

    private void validateFile(DocumentFile file) {
        if (file.size() <= 0 || file.size() > maxSizeBytes) {
            throw new InvalidDocumentSizeException(file.size(), maxSizeBytes);
        }

        String contentType = file.contentType() == null ? "" : file.contentType().toLowerCase(Locale.ROOT);
        if (!allowedContentTypes.contains(contentType)) {
            throw new InvalidDocumentContentTypeException(file.contentType());
        }
    }

    private String objectKey(UploadDocumentCommand command, UUID documentId) {
        String context = command.businessContext().toLowerCase(Locale.ROOT);
        return context + "/" + command.businessReferenceId() + "/" + command.category() + "/" + documentId;
    }
}
