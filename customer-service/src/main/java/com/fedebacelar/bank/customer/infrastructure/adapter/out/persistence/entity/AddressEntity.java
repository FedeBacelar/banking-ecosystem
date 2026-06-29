package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.customer.domain.enums.AddressType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
public class AddressEntity {

    @Id
    private String id;

    private String partyId;

    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    private String street;

    private String streetNumber;

    private String city;

    private String province;

    private String postalCode;

    private String country;

}
