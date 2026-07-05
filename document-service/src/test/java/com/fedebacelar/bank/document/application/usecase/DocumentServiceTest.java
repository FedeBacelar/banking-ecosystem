package com.fedebacelar.bank.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.document.application.command.UploadDocumentCommand;
import com.fedebacelar.bank.document.application.port.out.DocumentRepositoryPort;
import com.fedebacelar.bank.document.application.port.out.ObjectStoragePort;
import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.exception.DocumentNotFoundException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentContentTypeException;
import com.fedebacelar.bank.document.domain.exception.InvalidDocumentSizeException;
import com.fedebacelar.bank.document.domain.model.Document;
import com.fedebacelar.bank.document.domain.model.DocumentFile;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentServiceTest {

    private final DocumentRepositoryPort repositoryPort = mock(DocumentRepositoryPort.class);
    private final ObjectStoragePort objectStoragePort = mock(ObjectStoragePort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-04T21:00:00Z"), ZoneOffset.UTC);
    private final DocumentService service = new DocumentService(
            repositoryPort,
            objectStoragePort,
            clock,
            10L,
            Set.of("image/jpeg", "image/png", "application/pdf")
    );

    @Test
    void uploadsDocumentAndPersistsMetadata() {
        when(objectStoragePort.store(any(), any()))
                .thenReturn(new ObjectStoragePort.StoredObject("banking-documents", "onboarding/application-1/DNI_FRONT/document-id"));
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.upload(new UploadDocumentCommand(
                "ONBOARDING",
                "application-1",
                DocumentCategory.DNI_FRONT,
                file("image/jpeg", "dni-front.jpg", 4)
        ));

        assertThat(details.businessContext()).isEqualTo("ONBOARDING");
        assertThat(details.businessReferenceId()).isEqualTo("application-1");
        assertThat(details.category()).isEqualTo(DocumentCategory.DNI_FRONT);
        assertThat(details.status()).isEqualTo(DocumentStatus.STORED);
        assertThat(details.createdAt()).isEqualTo(Instant.parse("2026-07-04T21:00:00Z"));
        verify(objectStoragePort).store(eq("onboarding/application-1/DNI_FRONT/" + details.id()), any());
        verify(repositoryPort).save(any(Document.class));
    }

    @Test
    void rejectsUnsupportedContentType() {
        assertThatThrownBy(() -> service.upload(new UploadDocumentCommand(
                "ONBOARDING",
                "application-1",
                DocumentCategory.DNI_FRONT,
                file("text/plain", "note.txt", 4)
        ))).isInstanceOf(InvalidDocumentContentTypeException.class);
    }

    @Test
    void rejectsOversizedDocument() {
        assertThatThrownBy(() -> service.upload(new UploadDocumentCommand(
                "ONBOARDING",
                "application-1",
                DocumentCategory.DNI_FRONT,
                file("image/png", "dni-front.png", 11)
        ))).isInstanceOf(InvalidDocumentSizeException.class);
    }

    @Test
    void returnsDocumentMetadata() {
        UUID documentId = UUID.randomUUID();
        when(repositoryPort.findById(documentId)).thenReturn(Optional.of(document(documentId)));

        var details = service.get(documentId);

        assertThat(details.id()).isEqualTo(documentId);
        assertThat(details.objectKey()).isEqualTo("onboarding/application-1/DNI_FRONT/" + documentId);
    }

    @Test
    void failsWhenDocumentDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        when(repositoryPort.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(documentId))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    private DocumentFile file(String contentType, String originalFilename, int size) {
        return new DocumentFile(new ByteArrayInputStream(new byte[size]), size, contentType, originalFilename);
    }

    private Document document(UUID documentId) {
        Instant now = Instant.parse("2026-07-04T21:00:00Z");
        return new Document(
                documentId,
                "ONBOARDING",
                "application-1",
                DocumentCategory.DNI_FRONT,
                "dni-front.jpg",
                "image/jpeg",
                4L,
                com.fedebacelar.bank.document.domain.enums.DocumentStorageProvider.MINIO,
                "banking-documents",
                "onboarding/application-1/DNI_FRONT/" + documentId,
                DocumentStatus.STORED,
                now,
                now,
                0L
        );
    }
}
