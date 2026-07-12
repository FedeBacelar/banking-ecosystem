package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto.DocumentMetadataResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentValidationAdapterTest {
    @Mock
    private DocumentFeignClient client;

    @InjectMocks
    private DocumentValidationAdapter adapter;

    @Test
    void acceptsStoredDocumentOwnedByTheExpectedOnboardingApplicationAndCategory() {
        UUID documentId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        when(client.get(documentId)).thenReturn(new DocumentMetadataResponse(
                documentId,
                "ONBOARDING_APPLICATION",
                applicationId.toString(),
                "DNI_FRONT",
                "STORED"
        ));

        assertThat(adapter.isStoredOnboardingDocument(
                documentId, applicationId, OnboardingDocumentCategory.DNI_FRONT
        )).isTrue();
    }

    @Test
    void rejectsDocumentFromAnotherBusinessContext() {
        UUID documentId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        when(client.get(documentId)).thenReturn(new DocumentMetadataResponse(
                documentId,
                "CUSTOMER_PROFILE",
                applicationId.toString(),
                "DNI_FRONT",
                "STORED"
        ));

        assertThat(adapter.isStoredOnboardingDocument(
                documentId, applicationId, OnboardingDocumentCategory.DNI_FRONT
        )).isFalse();
    }
}
