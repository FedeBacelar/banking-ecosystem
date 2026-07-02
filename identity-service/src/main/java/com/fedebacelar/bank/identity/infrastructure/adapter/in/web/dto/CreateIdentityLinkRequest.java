package com.fedebacelar.bank.identity.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateIdentityLinkRequest(
        @NotNull UUID customerId,
        @NotNull IdentityProvider provider,
        @NotBlank @Size(max = 255) String providerSubject
) {
}
