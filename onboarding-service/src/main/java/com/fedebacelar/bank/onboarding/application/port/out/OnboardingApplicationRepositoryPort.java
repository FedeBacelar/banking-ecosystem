package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OnboardingApplicationRepositoryPort {

    OnboardingApplication save(OnboardingApplication application);

    Optional<OnboardingApplication> findById(UUID applicationId);

    Optional<OnboardingApplication> findByMagicLinkTokenHash(String tokenHash);

    Optional<OnboardingApplication> findByContinuationTokenHash(String tokenHash);

    Optional<OnboardingApplication> findFirstByEmailAndStatusInOrderByCreatedAtDesc(String email, Set<OnboardingApplicationStatus> statuses);

    boolean existsByEmailAndStatusIn(String email, Set<OnboardingApplicationStatus> statuses);
}
