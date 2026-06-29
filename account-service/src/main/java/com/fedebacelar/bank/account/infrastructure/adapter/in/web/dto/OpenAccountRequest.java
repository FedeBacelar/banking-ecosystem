package com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OpenAccountRequest(
        @NotNull UUID customerId,
        @NotNull AccountType type,
        @NotNull CurrencyCode currency,
        @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+([.-][a-z0-9]+){1,5}$", message = "must be a valid lowercase bank alias") String alias
) {
}
