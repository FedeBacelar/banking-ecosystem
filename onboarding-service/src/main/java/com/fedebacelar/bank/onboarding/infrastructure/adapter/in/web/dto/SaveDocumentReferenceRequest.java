package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaveDocumentReferenceRequest(
        @NotBlank @Size(max = 500) String continuationToken,
        @NotNull UUID documentId
) {
}
