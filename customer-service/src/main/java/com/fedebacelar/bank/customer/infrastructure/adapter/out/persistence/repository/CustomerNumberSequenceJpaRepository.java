package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerNumberSequenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerNumberSequenceJpaRepository extends JpaRepository<CustomerNumberSequenceEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from CustomerNumberSequenceEntity sequence where sequence.sequenceYear = :year")
    Optional<CustomerNumberSequenceEntity> findByYearForUpdate(@Param("year") Integer year);
}
