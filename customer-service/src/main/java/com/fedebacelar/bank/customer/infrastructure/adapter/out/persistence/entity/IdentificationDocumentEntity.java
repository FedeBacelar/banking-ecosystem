package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "identification_document")
@Getter
@Setter
@NoArgsConstructor
public class IdentificationDocumentEntity {

    @Id
    private String id;

    private String partyId;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String documentNumber;

    private String issuingCountry;

    private LocalDate expirationDate;

    private boolean primaryDocument;
}
