package com.fedebacelar.bank.customer.application.view;

import com.fedebacelar.bank.customer.domain.enums.AddressType;
import java.util.UUID;

public record AddressDetails(
        UUID id,
        AddressType type,
        String street,
        String streetNumber,
        String city,
        String province,
        String postalCode,
        String country
) {
}
