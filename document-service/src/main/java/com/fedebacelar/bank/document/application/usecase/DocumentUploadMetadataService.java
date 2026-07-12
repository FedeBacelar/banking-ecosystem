package com.fedebacelar.bank.document.application.usecase;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.port.out.DocumentRepositoryPort;
import com.fedebacelar.bank.document.domain.exception.DocumentIdempotencyConflictException;
import com.fedebacelar.bank.document.domain.exception.DocumentNotFoundException;
import com.fedebacelar.bank.document.domain.model.Document;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DocumentUploadMetadataService {

    private final DocumentRepositoryPort repository;
    private final Clock clock;

    public DocumentUploadMetadataService(DocumentRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Document reserve(UploadDocumentCommand command, String bucketName) {
        return repository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> validateReplay(existing, command))
                .orElseGet(() -> createReservation(command, bucketName));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<Document> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Document markStored(UUID documentId) {
        Document document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        if (document.status() == com.fedebacelar.bank.document.domain.enums.DocumentStatus.STORED) {
            return document;
        }
        return repository.save(document.markStored(Instant.now(clock)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Document markFailed(UUID documentId) {
        Document document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return repository.save(document.markFailed(Instant.now(clock)));
    }

    private Document createReservation(UploadDocumentCommand command, String bucketName) {
        UUID documentId = UUID.nameUUIDFromBytes(
                ("document:" + command.idempotencyKey()).getBytes(StandardCharsets.UTF_8)
        );
        String objectKey = command.businessContext().toLowerCase(Locale.ROOT)
                + "/" + command.businessReferenceId()
                + "/" + command.category()
                + "/" + documentId;
        return repository.save(Document.pending(
                documentId,
                command.idempotencyKey(),
                command.contentSha256(),
                command.businessContext(),
                command.businessReferenceId(),
                command.category(),
                command.file().originalFilename(),
                command.file().contentType(),
                command.file().size(),
                bucketName,
                objectKey,
                Instant.now(clock)
        ));
    }

    private Document validateReplay(Document existing, UploadDocumentCommand command) {
        if (!existing.contentSha256().equals(command.contentSha256())
                || !existing.businessContext().equals(command.businessContext())
                || !existing.businessReferenceId().equals(command.businessReferenceId())
                || existing.category() != command.category()) {
            throw new DocumentIdempotencyConflictException();
        }
        return existing;
    }
}
