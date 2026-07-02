package com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence.entity;

import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "identity_link")
@Getter
@Setter
@NoArgsConstructor
public class IdentityLinkEntity {

    @Id
    private String id;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private IdentityProvider provider;

    private String providerSubject;

    @Enumerated(EnumType.STRING)
    private IdentityLinkStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;
}
