package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingApplicationEntity;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingApplicationJpaRepository extends JpaRepository<OnboardingApplicationEntity, String> {

    Optional<OnboardingApplicationEntity> findByMagicLinkTokenHash(String magicLinkTokenHash);

    Optional<OnboardingApplicationEntity> findByContinuationTokenHash(String continuationTokenHash);

    Optional<OnboardingApplicationEntity> findFirstByEmailAndStatusInOrderByCreatedAtDesc(String email, Set<OnboardingApplicationStatus> statuses);

    boolean existsByEmailAndStatusIn(String email, Set<OnboardingApplicationStatus> statuses);
}
