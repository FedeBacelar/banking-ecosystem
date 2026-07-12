package com.fedebacelar.bank.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.port.out.DocumentRepositoryPort;
import com.fedebacelar.bank.document.application.port.out.ObjectStoragePort;
import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.exception.DocumentNotFoundException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentContentException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentContentTypeException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentHashException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentSizeException;
import com.fedebacelar.bank.document.domain.model.Document;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final String CONTENT_HASH = "a".repeat(64);

    private final DocumentRepositoryPort repository = mock(DocumentRepositoryPort.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final DocumentUploadMetadataService metadata = mock(DocumentUploadMetadataService.class);
    private final DocumentService service = new DocumentService(
            repository,
            storage,
            metadata,
            10L,
            Set.of("image/jpeg", "image/png", "application/pdf")
    );

    @BeforeEach
    void setUp() {
        when(storage.bucketName()).thenReturn("banking-documents");
    }

    @Test
    void shouldStoreObjectThenCommitStoredMetadata() {
        UploadDocumentCommand command = command(validPng());
        Document pending = pendingDocument();
        Document stored = pending.markStored(NOW.plusSeconds(1));
        when(metadata.reserve(command, "banking-documents")).thenReturn(pending);
        when(storage.store(pending.objectKey(), command.file())).thenReturn(new ObjectStoragePort.StoredObject(
                "banking-documents", pending.objectKey(), CONTENT_HASH
        ));
        when(metadata.markStored(pending.id())).thenReturn(stored);

        var result = service.upload(command);

        assertThat(result.status()).isEqualTo(DocumentStatus.STORED);
        assertThat(result.contentSha256()).isEqualTo(CONTENT_HASH);
        verify(storage).store(pending.objectKey(), command.file());
        verify(metadata).markStored(pending.id());
    }

    @Test
    void shouldReturnStoredMetadataForAnIdempotentReplayWithoutTouchingMinio() {
        UploadDocumentCommand command = command(validPng());
        Document stored = pendingDocument().markStored(NOW);
        when(metadata.reserve(command, "banking-documents")).thenReturn(stored);

        var result = service.upload(command);

        assertThat(result.id()).isEqualTo(stored.id());
        verify(storage, never()).store(any(), any());
    }

    @Test
    void shouldMarkMetadataFailedAndPreserveCleanupFailureWhenHashDoesNotMatch() {
        UploadDocumentCommand command = command(validPng());
        Document pending = pendingDocument();
        RuntimeException cleanupFailure = new RuntimeException("MinIO delete failed");
        when(metadata.reserve(command, "banking-documents")).thenReturn(pending);
        when(storage.store(pending.objectKey(), command.file())).thenReturn(new ObjectStoragePort.StoredObject(
                "banking-documents", pending.objectKey(), "b".repeat(64)
        ));
        org.mockito.Mockito.doThrow(cleanupFailure).when(storage).delete(pending.objectKey());
        when(metadata.markFailed(pending.id())).thenReturn(pending.markFailed(NOW));

        assertThatThrownBy(() -> service.upload(command))
                .isInstanceOf(InvalidDocumentHashException.class)
                .satisfies(error -> assertThat(error.getSuppressed()).contains(cleanupFailure));

        verify(metadata).markFailed(pending.id());
    }

    @Test
    void shouldRejectSpoofedContentTypeBeforeCreatingMetadata() {
        DocumentFile spoofed = file(new byte[]{1, 2, 3, 4}, "image/png", "fake.png");

        assertThatThrownBy(() -> service.upload(command(spoofed)))
                .isInstanceOf(InvalidDocumentContentException.class);

        verifyNoInteractions(metadata);
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        DocumentFile file = file(new byte[]{1, 2, 3, 4}, "text/plain", "note.txt");

        assertThatThrownBy(() -> service.upload(command(file)))
                .isInstanceOf(InvalidDocumentContentTypeException.class);
    }

    @Test
    void shouldRejectOversizedDocument() {
        byte[] bytes = new byte[11];
        System.arraycopy(validPngBytes(), 0, bytes, 0, 8);

        assertThatThrownBy(() -> service.upload(command(file(bytes, "image/png", "large.png"))))
                .isInstanceOf(InvalidDocumentSizeException.class);
    }

    @Test
    void shouldReturnDocumentMetadata() {
        Document document = pendingDocument().markStored(NOW);
        when(repository.findById(document.id())).thenReturn(Optional.of(document));

        assertThat(service.get(document.id()).objectKey()).isEqualTo(document.objectKey());
    }

    @Test
    void shouldFailWhenDocumentDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        when(repository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(documentId)).isInstanceOf(DocumentNotFoundException.class);
    }

    private UploadDocumentCommand command(DocumentFile file) {
        return new UploadDocumentCommand(
                "onboarding:application-1:DNI_FRONT:" + CONTENT_HASH,
                CONTENT_HASH,
                "ONBOARDING_APPLICATION",
                "application-1",
                DocumentCategory.DNI_FRONT,
                file
        );
    }

    private Document pendingDocument() {
        UUID id = UUID.nameUUIDFromBytes(("document:onboarding:application-1:DNI_FRONT:" + CONTENT_HASH).getBytes());
        return Document.pending(
                id,
                "onboarding:application-1:DNI_FRONT:" + CONTENT_HASH,
                CONTENT_HASH,
                "ONBOARDING_APPLICATION",
                "application-1",
                DocumentCategory.DNI_FRONT,
                "dni-front.png",
                "image/png",
                8,
                "banking-documents",
                "onboarding_application/application-1/DNI_FRONT/" + id,
                NOW
        );
    }

    private DocumentFile validPng() {
        return file(validPngBytes(), "image/png", "dni-front.png");
    }

    private byte[] validPngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private DocumentFile file(byte[] bytes, String contentType, String filename) {
        return new DocumentFile(() -> new ByteArrayInputStream(bytes), bytes.length, contentType, filename);
    }
}
