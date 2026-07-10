package com.fedebacelar.bank.homebanking.bff.application.port.out;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocument;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentServicePort {

    OnboardingDocument uploadOnboardingDocument(UUID applicationId, String category, MultipartFile file, String accessToken);
}
