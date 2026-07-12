package com.fedebacelar.bank.document.application.usecase;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.mapper.DocumentDetailsMapper;
import com.fedebacelar.bank.document.application.port.in.GetDocumentUseCase;
import com.fedebacelar.bank.document.application.port.in.UploadDocumentUseCase;
import com.fedebacelar.bank.document.application.port.out.DocumentRepositoryPort;
import com.fedebacelar.bank.document.application.port.out.ObjectStoragePort;
import com.fedebacelar.bank.document.application.view.DocumentDetails;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.exception.DocumentIdempotencyConflictException;
import com.fedebacelar.bank.document.domain.exception.DocumentNotFoundException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentContentTypeException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentContentException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentHashException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentSizeException;
import com.fedebacelar.bank.document.domain.model.Document;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService implements UploadDocumentUseCase, GetDocumentUseCase {

    private final DocumentRepositoryPort repositoryPort;
    private final ObjectStoragePort objectStoragePort;
    private final DocumentUploadMetadataService metadataService;
    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public DocumentService(
            DocumentRepositoryPort repositoryPort,
            ObjectStoragePort objectStoragePort,
            DocumentUploadMetadataService metadataService,
            @Value("${document.upload.max-size-bytes:10485760}") long maxSizeBytes,
            @Value("${document.upload.allowed-content-types:image/jpeg,image/png,application/pdf}") Set<String> allowedContentTypes
    ) {
        this.repositoryPort = repositoryPort;
        this.objectStoragePort = objectStoragePort;
        this.metadataService = metadataService;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedContentTypes = allowedContentTypes;
    }

    @Override
    public DocumentDetails upload(UploadDocumentCommand command) {
        validateFile(command.file());
        Document reserved = reserve(command);
        if (reserved.status() == DocumentStatus.STORED) {
            return DocumentDetailsMapper.toDetails(reserved);
        }

        try {
            ObjectStoragePort.StoredObject stored = objectStoragePort.store(reserved.objectKey(), command.file());
            if (!command.contentSha256().equals(stored.contentSha256())) {
                InvalidDocumentHashException mismatch = new InvalidDocumentHashException();
                try {
                    objectStoragePort.delete(reserved.objectKey());
                } catch (RuntimeException cleanupFailure) {
                    mismatch.addSuppressed(cleanupFailure);
                }
                markFailedPreserving(mismatch, reserved.id());
                throw mismatch;
            }
            return DocumentDetailsMapper.toDetails(metadataService.markStored(reserved.id()));
        } catch (RuntimeException exception) {
            if (!(exception instanceof InvalidDocumentHashException)) {
                markFailedPreserving(exception, reserved.id());
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDetails get(UUID documentId) {
        Document document = repositoryPort.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return DocumentDetailsMapper.toDetails(document);
    }

    private Document reserve(UploadDocumentCommand command) {
        try {
            return metadataService.reserve(command, objectStoragePort.bucketName());
        } catch (DataIntegrityViolationException concurrentReservation) {
            return metadataService.findByIdempotencyKey(command.idempotencyKey())
                    .map(existing -> validateIdempotentReplay(existing, command))
                    .orElseThrow(() -> concurrentReservation);
        }
    }

    private Document validateIdempotentReplay(Document existing, UploadDocumentCommand command) {
        if (!existing.contentSha256().equals(command.contentSha256())
                || !existing.businessContext().equals(command.businessContext())
                || !existing.businessReferenceId().equals(command.businessReferenceId())
                || existing.category() != command.category()) {
            throw new DocumentIdempotencyConflictException();
        }
        return existing;
    }

    private void markFailedPreserving(RuntimeException original, UUID documentId) {
        try {
            metadataService.markFailed(documentId);
        } catch (RuntimeException metadataFailure) {
            original.addSuppressed(metadataFailure);
        }
    }

    private void validateFile(DocumentFile file) {
        if (file.size() <= 0 || file.size() > maxSizeBytes) {
            throw new InvalidDocumentSizeException(file.size(), maxSizeBytes);
        }

        String contentType = file.contentType() == null ? "" : file.contentType().toLowerCase(Locale.ROOT);
        if (!allowedContentTypes.contains(contentType)) {
            throw new InvalidDocumentContentTypeException(file.contentType());
        }
        validateSignature(file, contentType);
    }

    private void validateSignature(DocumentFile file, String contentType) {
        byte[] signature = new byte[8];
        int length;
        try (InputStream input = file.openStream()) {
            length = input.read(signature);
        } catch (IOException exception) {
            throw new InvalidDocumentContentException(exception);
        }

        boolean matches = switch (contentType) {
            case "image/jpeg" -> startsWith(signature, length, new int[]{0xFF, 0xD8, 0xFF});
            case "image/png" -> startsWith(signature, length, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "application/pdf" -> startsWith(signature, length, new int[]{0x25, 0x50, 0x44, 0x46, 0x2D});
            default -> false;
        };
        if (!matches) {
            throw new InvalidDocumentContentException();
        }
    }

    private boolean startsWith(byte[] actual, int length, int[] expected) {
        if (length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((actual[index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }
}
