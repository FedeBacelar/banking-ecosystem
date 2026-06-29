package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountNumberSequenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountNumberSequenceJpaRepository extends JpaRepository<AccountNumberSequenceEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AccountNumberSequenceEntity s where s.year = :year")
    Optional<AccountNumberSequenceEntity> findByYearForUpdate(@Param("year") Integer year);
}
