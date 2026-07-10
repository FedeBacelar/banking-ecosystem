package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.document.dto;

import java.util.UUID;

public record DocumentMetadataResponse(
        UUID id,
        String businessContext,
        String businessReferenceId,
        String category,
        String status
) {
}
