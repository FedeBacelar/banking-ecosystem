package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.customer.domain.enums.ContactType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_point")
@Getter
@Setter
@NoArgsConstructor
public class ContactPointEntity {

    @Id
    private String id;

    private String partyId;

    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    private String contactValue;

    private boolean verified;

    private boolean primaryContact;

}
