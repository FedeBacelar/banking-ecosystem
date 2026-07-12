package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.UniquenessReservationType;
import java.time.Instant;
import java.util.UUID;

public interface OnboardingUniquenessReservationPort {
    boolean tryAcquire(UniquenessReservationType type, String normalizedValue, UUID applicationId, Instant now);
    void releaseByApplicationId(UUID applicationId, Instant now);
    void convertByApplicationId(UUID applicationId, Instant now);
}
