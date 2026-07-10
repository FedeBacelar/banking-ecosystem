package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import java.util.UUID;

public record SaveDocumentReferenceRequest(
        String continuationToken,
        UUID documentId
) {
}
