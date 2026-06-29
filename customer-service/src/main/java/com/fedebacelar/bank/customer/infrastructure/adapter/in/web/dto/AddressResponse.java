package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.customer.domain.enums.AddressType;
import java.util.UUID;

public record AddressResponse(
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
