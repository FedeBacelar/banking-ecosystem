package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import java.util.Optional;
import java.util.UUID;

public interface MagicLinkDeliveryRepositoryPort {
    Optional<MagicLinkDelivery> findByApplicationId(UUID applicationId);

    MagicLinkDelivery save(MagicLinkDelivery delivery);
}
