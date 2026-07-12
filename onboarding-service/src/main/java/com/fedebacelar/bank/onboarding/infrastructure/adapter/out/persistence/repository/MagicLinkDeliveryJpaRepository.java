package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.MagicLinkDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MagicLinkDeliveryJpaRepository extends JpaRepository<MagicLinkDeliveryEntity, String> {
}
