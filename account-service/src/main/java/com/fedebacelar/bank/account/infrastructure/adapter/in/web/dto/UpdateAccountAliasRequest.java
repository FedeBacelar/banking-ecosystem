package com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountAliasRequest(
        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+([.-][a-z0-9]+){1,5}$", message = "must be a valid lowercase bank alias")
        String alias
) {
}
