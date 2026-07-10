package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;

public interface CustomerDuplicateLookupPort {
    boolean existsByDocument(ApplicantDocumentType type, String number, String country);
    boolean existsByEmail(String email);
}
