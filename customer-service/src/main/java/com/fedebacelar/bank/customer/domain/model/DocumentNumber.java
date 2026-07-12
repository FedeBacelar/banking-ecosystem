package com.fedebacelar.bank.customer.domain.model;

import java.util.Locale;

public final class DocumentNumber {

    private DocumentNumber() {
    }

    public static String canonical(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
