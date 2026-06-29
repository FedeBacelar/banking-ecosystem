package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "natural_person")
@Getter
@Setter
@NoArgsConstructor
public class NaturalPersonEntity {

    @Id
    private String partyId;

    private String firstName;

    private String middleName;

    private String lastName;

    private LocalDate birthDate;

    private String nationality;
}
