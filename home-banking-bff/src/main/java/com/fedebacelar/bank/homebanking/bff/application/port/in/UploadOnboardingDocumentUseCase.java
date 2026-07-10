package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import org.springframework.web.multipart.MultipartFile;

public interface UploadOnboardingDocumentUseCase {

    OnboardingDocumentReference uploadDocument(String continuationToken, String category, MultipartFile file);
}
