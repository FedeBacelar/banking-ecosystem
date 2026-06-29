package com.fedebacelar.bank.customer.domain.model;

import com.fedebacelar.bank.customer.domain.enums.AddressType;
import java.util.UUID;

public record Address(
        UUID id,
        UUID partyId,
        AddressType type,
        String street,
        String streetNumber,
        String city,
        String province,
        String postalCode,
        String country
) {
}
