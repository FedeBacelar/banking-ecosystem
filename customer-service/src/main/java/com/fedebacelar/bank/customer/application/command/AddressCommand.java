package com.fedebacelar.bank.customer.application.command;

import com.fedebacelar.bank.customer.domain.enums.AddressType;

public record AddressCommand(
        AddressType type,
        String street,
        String streetNumber,
        String city,
        String province,
        String postalCode,
        String country
) {
}
