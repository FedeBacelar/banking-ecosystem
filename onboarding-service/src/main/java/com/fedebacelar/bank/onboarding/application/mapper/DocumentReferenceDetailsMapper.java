package com.fedebacelar.bank.onboarding.application.mapper;

import com.fedebacelar.bank.onboarding.application.view.DocumentReferenceDetails;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;

public final class DocumentReferenceDetailsMapper {

    private DocumentReferenceDetailsMapper() {
    }

    public static DocumentReferenceDetails toDetails(OnboardingDocumentReference reference) {
        return new DocumentReferenceDetails(
                reference.id(),
                reference.applicationId(),
                reference.category(),
                reference.documentId(),
                reference.createdAt(),
                reference.updatedAt()
        );
    }
}
