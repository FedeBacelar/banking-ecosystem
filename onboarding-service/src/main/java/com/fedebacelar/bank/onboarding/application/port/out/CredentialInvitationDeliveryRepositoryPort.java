package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialInvitationDeliveryRepositoryPort {
    CredentialInvitationDelivery save(CredentialInvitationDelivery delivery);

    Optional<CredentialInvitationDelivery> findByApplicationIdAndIdempotencyKeyHash(
            UUID applicationId,
            String idempotencyKeyHash
    );

    Optional<CredentialInvitationDelivery> findLatestByApplicationId(UUID applicationId);

    List<CredentialInvitationDelivery> findActiveByApplicationIdForUpdate(UUID applicationId);

    Optional<CredentialInvitationDelivery> claimNext(Instant now, Duration lease);
}
