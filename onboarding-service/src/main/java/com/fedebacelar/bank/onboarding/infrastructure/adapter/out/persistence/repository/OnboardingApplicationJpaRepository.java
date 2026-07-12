package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingApplicationEntity;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface OnboardingApplicationJpaRepository extends JpaRepository<OnboardingApplicationEntity, String> {

    Optional<OnboardingApplicationEntity> findByMagicLinkTokenHash(String magicLinkTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from OnboardingApplicationEntity application where application.magicLinkTokenHash = :tokenHash")
    Optional<OnboardingApplicationEntity> findByMagicLinkTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<OnboardingApplicationEntity> findByContinuationTokenHash(String continuationTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from OnboardingApplicationEntity application where application.continuationTokenHash = :tokenHash")
    Optional<OnboardingApplicationEntity> findByContinuationTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<OnboardingApplicationEntity> findFirstByEmailAndStatusInOrderByCreatedAtDesc(String email, Set<OnboardingApplicationStatus> statuses);

    boolean existsByEmailAndStatusIn(String email, Set<OnboardingApplicationStatus> statuses);

    boolean existsByEmailAndStatusInAndIdNot(String email, Set<OnboardingApplicationStatus> statuses, String id);

    List<OnboardingApplicationEntity> findByExpiresAtLessThanEqualAndStatusInOrderByExpiresAtAsc(
            Instant now, Set<OnboardingApplicationStatus> statuses, Pageable pageable
    );
}
