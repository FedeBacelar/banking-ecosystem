package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import com.fedebacelar.bank.onboarding.application.port.out.DocumentValidationPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto.DocumentMetadataResponse;
import feign.FeignException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentValidationAdapter implements DocumentValidationPort {
    private static final String ONBOARDING_BUSINESS_CONTEXT = "ONBOARDING_APPLICATION";

    private final DocumentFeignClient client;

    public DocumentValidationAdapter(DocumentFeignClient client) {
        this.client = client;
    }

    @Override
    public boolean isStoredOnboardingDocument(UUID documentId, UUID applicationId, OnboardingDocumentCategory category) {
        try {
            DocumentMetadataResponse document = client.get(documentId);
            return document != null
                    && ONBOARDING_BUSINESS_CONTEXT.equals(document.businessContext())
                    && applicationId.toString().equals(document.businessReferenceId())
                    && category.name().equals(document.category())
                    && "STORED".equals(document.status());
        } catch (FeignException.NotFound ignored) {
            return false;
        }
    }
}
