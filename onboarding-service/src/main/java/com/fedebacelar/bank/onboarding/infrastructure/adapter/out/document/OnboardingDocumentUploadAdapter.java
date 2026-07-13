package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentUploadPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingDocumentException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentTooLargeException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentUploadException;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto.DocumentMetadataResponse;
import feign.FeignException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingDocumentUploadAdapter implements OnboardingDocumentUploadPort {

    private static final String BUSINESS_CONTEXT = "ONBOARDING_APPLICATION";

    private final DocumentFeignClient client;

    public OnboardingDocumentUploadAdapter(DocumentFeignClient client) {
        this.client = client;
    }

    @Override
    public UUID upload(
            UUID applicationId,
            OnboardingDocumentCategory category,
            OnboardingDocumentUpload document,
            String contentSha256
    ) {
        String idempotencyKey = "onboarding:%s:%s:%s".formatted(applicationId, category, contentSha256);
        DocumentMetadataResponse response;
        try {
            response = client.upload(
                    idempotencyKey,
                    contentSha256,
                    BUSINESS_CONTEXT,
                    applicationId.toString(),
                    category.name(),
                    new OnboardingMultipartFile(document)
            );
        } catch (FeignException exception) {
            if (exception.status() == 400) {
                throw new InvalidOnboardingDocumentException(exception);
            }
            if (exception.status() == 413) {
                throw new OnboardingDocumentTooLargeException(exception);
            }
            throw new OnboardingDocumentUploadException("Document storage is unavailable.", exception);
        }
        if (response == null || !"STORED".equals(response.status())) {
            throw new OnboardingDocumentUploadException("Document storage did not complete.");
        }
        return response.id();
    }
}
