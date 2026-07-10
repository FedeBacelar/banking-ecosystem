package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.document;

import com.fedebacelar.bank.homebanking.bff.application.port.out.DocumentServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocument;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.document.dto.DocumentResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DocumentServiceAdapter implements DocumentServicePort {

    private static final String BUSINESS_CONTEXT = "ONBOARDING_APPLICATION";

    private final WebClient webClient;

    public DocumentServiceAdapter(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.build();
    }

    @Override
    public OnboardingDocument uploadOnboardingDocument(UUID applicationId, String category, MultipartFile file, String accessToken) {
        DocumentResponse response = webClient.post()
                .uri("http://document-service/internal/documents")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBody(applicationId, category, file)))
                .retrieve()
                .bodyToMono(DocumentResponse.class)
                .block();

        return response.toDomain();
    }

    private org.springframework.util.MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartBody(
            UUID applicationId,
            String category,
            MultipartFile file
    ) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("businessContext", BUSINESS_CONTEXT);
        builder.part("businessReferenceId", applicationId.toString());
        builder.part("category", category);
        builder.part("file", fileResource(file))
                .filename(originalFilename(file))
                .contentType(fileContentType(file));
        return builder.build();
    }

    private ByteArrayResource fileResource(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return originalFilename(file);
                }
            };
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read onboarding document upload", exception);
        }
    }

    private String originalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "document";
        }
        return originalFilename;
    }

    private MediaType fileContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }
}
