package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.account.application.port.out.CbuGeneratorPort;
import com.fedebacelar.bank.account.infrastructure.config.BankingIdentifierProperties;
import org.springframework.stereotype.Component;

@Component
public class CbuGeneratorAdapter implements CbuGeneratorPort {

    private final BankingIdentifierProperties properties;

    public CbuGeneratorAdapter(BankingIdentifierProperties properties) {
        this.properties = properties;
    }

    @Override
    public String nextCbu(String accountNumber) {
        String numericPart = accountNumber.replaceAll("\\D", "");
        String padded = String.format("%014d", Long.parseLong(numericPart));
        String base = properties.bankCode() + properties.defaultBranchCode() + padded.substring(padded.length() - 14);
        return base + checksum(base);
    }

    private int checksum(String value) {
        int sum = 0;
        for (int index = 0; index < value.length(); index++) {
            sum += Character.digit(value.charAt(index), 10) * (index + 1);
        }
        return sum % 10;
    }
}
