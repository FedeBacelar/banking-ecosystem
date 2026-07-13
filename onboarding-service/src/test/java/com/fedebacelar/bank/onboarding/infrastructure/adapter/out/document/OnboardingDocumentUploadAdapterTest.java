package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingDocumentException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentUploadException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentTooLargeException;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto.DocumentMetadataResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class OnboardingDocumentUploadAdapterTest {

    @Mock
    private DocumentFeignClient client;

    @Captor
    private ArgumentCaptor<MultipartFile> fileCaptor;

    @InjectMocks
    private OnboardingDocumentUploadAdapter adapter;

    @Test
    void sendsTheDocumentAsTheNamedMultipartFilePart() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        byte[] content = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        OnboardingDocumentUpload upload = new OnboardingDocumentUpload(
                "dni-front.png", "image/png", content.length, () -> new ByteArrayInputStream(content)
        );
        String hash = "a".repeat(64);
        when(client.upload(
                eq("onboarding:%s:DNI_FRONT:%s".formatted(applicationId, hash)),
                eq(hash),
                eq("ONBOARDING_APPLICATION"),
                eq(applicationId.toString()),
                eq("DNI_FRONT"),
                fileCaptor.capture()
        )).thenReturn(new DocumentMetadataResponse(
                documentId, "ONBOARDING_APPLICATION", applicationId.toString(), "DNI_FRONT", "STORED"
        ));

        UUID result = adapter.upload(applicationId, OnboardingDocumentCategory.DNI_FRONT, upload, hash);

        MultipartFile file = fileCaptor.getValue();
        assertThat(result).isEqualTo(documentId);
        assertThat(file.getName()).isEqualTo("file");
        assertThat(file.getOriginalFilename()).isEqualTo("dni-front.png");
        assertThat(file.getContentType()).isEqualTo("image/png");
        assertThat(file.getBytes()).isEqualTo(content);
    }

    @Test
    void translatesDocumentServiceFailuresToTheApplicationException() {
        UUID applicationId = UUID.randomUUID();
        OnboardingDocumentUpload upload = new OnboardingDocumentUpload(
                "dni-front.png", "image/png", 0, () -> new ByteArrayInputStream(new byte[0])
        );
        FeignException remoteFailure = FeignException.errorStatus(
                "upload",
                Response.builder()
                        .status(503)
                        .reason("Service Unavailable")
                        .request(Request.create(
                                Request.HttpMethod.POST,
                                "http://document-service/internal/documents",
                                Map.of(),
                                null,
                                StandardCharsets.UTF_8,
                                null
                        ))
                        .build()
        );
        when(client.upload(any(), any(), any(), any(), any(), any())).thenThrow(remoteFailure);

        assertThatThrownBy(() -> adapter.upload(
                applicationId, OnboardingDocumentCategory.DNI_FRONT, upload, "a".repeat(64)
        )).isInstanceOf(OnboardingDocumentUploadException.class)
                .hasCause(remoteFailure);
    }

    @Test
    void translatesDocumentServiceBadRequestsToTheInvalidDocumentException() {
        UUID applicationId = UUID.randomUUID();
        OnboardingDocumentUpload upload = new OnboardingDocumentUpload(
                "dni-front.png", "image/png", 4, () -> new ByteArrayInputStream(new byte[]{1, 2, 3, 4})
        );
        FeignException remoteFailure = FeignException.errorStatus(
                "upload",
                Response.builder()
                        .status(400)
                        .reason("Bad Request")
                        .request(Request.create(
                                Request.HttpMethod.POST,
                                "http://document-service/internal/documents",
                                Map.of(),
                                null,
                                StandardCharsets.UTF_8,
                                null
                        ))
                        .build()
        );
        when(client.upload(any(), any(), any(), any(), any(), any())).thenThrow(remoteFailure);

        assertThatThrownBy(() -> adapter.upload(
                applicationId, OnboardingDocumentCategory.DNI_FRONT, upload, "a".repeat(64)
        )).isInstanceOf(InvalidOnboardingDocumentException.class)
                .hasCause(remoteFailure);
    }

    @Test
    void preservesDocumentServicePayloadTooLargeAsASeparateContract() {
        UUID applicationId = UUID.randomUUID();
        OnboardingDocumentUpload upload = new OnboardingDocumentUpload(
                "dni-front.png", "image/png", 4, () -> new ByteArrayInputStream(new byte[]{1, 2, 3, 4})
        );
        FeignException remoteFailure = FeignException.errorStatus(
                "upload",
                Response.builder()
                        .status(413)
                        .reason("Payload Too Large")
                        .request(Request.create(
                                Request.HttpMethod.POST,
                                "http://document-service/internal/documents",
                                Map.of(),
                                null,
                                StandardCharsets.UTF_8,
                                null
                        ))
                        .build()
        );
        when(client.upload(any(), any(), any(), any(), any(), any())).thenThrow(remoteFailure);

        assertThatThrownBy(() -> adapter.upload(
                applicationId, OnboardingDocumentCategory.DNI_FRONT, upload, "a".repeat(64)
        )).isInstanceOf(OnboardingDocumentTooLargeException.class)
                .hasCause(remoteFailure);
    }
}
