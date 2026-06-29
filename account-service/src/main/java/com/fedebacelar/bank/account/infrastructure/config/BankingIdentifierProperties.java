package com.fedebacelar.bank.account.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "banking.identifiers")
public record BankingIdentifierProperties(
        @NotBlank @Pattern(regexp = "\\d{3}") String bankCode,
        @NotBlank @Pattern(regexp = "\\d{4}") String defaultBranchCode
) {
}
