package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import java.util.UUID;

public interface OnboardingDocumentUploadPort {

    UUID upload(
            UUID applicationId,
            OnboardingDocumentCategory category,
            OnboardingDocumentUpload document,
            String contentSha256
    );
}
