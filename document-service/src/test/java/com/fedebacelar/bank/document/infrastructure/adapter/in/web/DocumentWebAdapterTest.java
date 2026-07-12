package com.fedebacelar.bank.document.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.document.application.port.in.GetDocumentUseCase;
import com.fedebacelar.bank.document.application.port.in.UploadDocumentUseCase;
import com.fedebacelar.bank.document.application.view.DocumentDetails;
import com.fedebacelar.bank.document.domain.enums.DocumentCategory;
import com.fedebacelar.bank.document.domain.enums.DocumentStatus;
import com.fedebacelar.bank.document.domain.enums.DocumentStorageProvider;
import com.fedebacelar.bank.document.domain.exception.DocumentNotFoundException;
import com.fedebacelar.bank.document.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.document.infrastructure.adapter.in.web.mapper.DocumentWebMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = DocumentController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DocumentWebMapper.class, GlobalExceptionHandler.class})
class DocumentWebAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadDocumentUseCase uploadDocumentUseCase;

    @MockitoBean
    private GetDocumentUseCase getDocumentUseCase;

    @Test
    void uploadsDocument() throws Exception {
        UUID documentId = UUID.randomUUID();
        when(uploadDocumentUseCase.upload(any())).thenReturn(documentDetails(documentId));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dni-front.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/internal/documents")
                        .file(file)
                        .header("Idempotency-Key", "onboarding:application-1:DNI_FRONT:" + "a".repeat(64))
                        .header("X-Content-SHA256", "a".repeat(64))
                        .param("businessContext", "ONBOARDING")
                        .param("businessReferenceId", "application-1")
                        .param("category", "DNI_FRONT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.category").value("DNI_FRONT"))
                .andExpect(jsonPath("$.status").value("STORED"));
    }

    @Test
    void returnsBadRequestForInvalidBusinessContext() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dni-front.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/internal/documents")
                        .file(file)
                        .header("Idempotency-Key", "onboarding:application-1:DNI_FRONT:" + "a".repeat(64))
                        .header("X-Content-SHA256", "a".repeat(64))
                        .param("businessContext", "onboarding")
                        .param("businessReferenceId", "application-1")
                        .param("category", "DNI_FRONT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void getsDocumentMetadata() throws Exception {
        UUID documentId = UUID.randomUUID();
        when(getDocumentUseCase.get(documentId)).thenReturn(documentDetails(documentId));

        mockMvc.perform(get("/internal/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.objectKey").value("onboarding/application-1/DNI_FRONT/" + documentId));
    }

    @Test
    void returnsNotFoundWhenDocumentDoesNotExist() throws Exception {
        UUID documentId = UUID.randomUUID();
        when(getDocumentUseCase.get(documentId)).thenThrow(new DocumentNotFoundException(documentId));

        mockMvc.perform(get("/internal/documents/{documentId}", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Document not found"));
    }

    private DocumentDetails documentDetails(UUID documentId) {
        Instant now = Instant.parse("2026-07-04T21:00:00Z");
        return new DocumentDetails(
                documentId,
                "a".repeat(64),
                "ONBOARDING",
                "application-1",
                DocumentCategory.DNI_FRONT,
                "dni-front.jpg",
                "image/jpeg",
                4L,
                DocumentStorageProvider.MINIO,
                "banking-documents",
                "onboarding/application-1/DNI_FRONT/" + documentId,
                DocumentStatus.STORED,
                now,
                now
        );
    }
}
