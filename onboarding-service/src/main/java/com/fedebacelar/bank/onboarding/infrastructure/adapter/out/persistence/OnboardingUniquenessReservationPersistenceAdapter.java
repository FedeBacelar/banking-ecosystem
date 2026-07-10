package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.domain.enums.UniquenessReservationType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OnboardingUniquenessReservationPersistenceAdapter implements OnboardingUniquenessReservationPort {
    private final JdbcTemplate jdbcTemplate;
    public OnboardingUniquenessReservationPersistenceAdapter(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(UniquenessReservationType type, String value, UUID applicationId, Instant now) {
        int inserted = jdbcTemplate.update("""
                INSERT IGNORE INTO onboarding_uniqueness_reservation
                    (id, reservation_type, normalized_value, application_id, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)
                """, UUID.randomUUID().toString(), type.name(), value, applicationId.toString(), Timestamp.from(now), Timestamp.from(now));
        if (inserted == 1) return true;

        Map<String, Object> existing = jdbcTemplate.queryForMap("""
                SELECT application_id, status FROM onboarding_uniqueness_reservation
                WHERE reservation_type = ? AND normalized_value = ?
                """, type.name(), value);
        if (applicationId.toString().equals(existing.get("application_id")) && !"RELEASED".equals(existing.get("status"))) {
            return true;
        }
        if ("RELEASED".equals(existing.get("status"))) {
            return jdbcTemplate.update("""
                    UPDATE onboarding_uniqueness_reservation
                       SET application_id = ?, status = 'ACTIVE', updated_at = ?
                     WHERE reservation_type = ? AND normalized_value = ? AND status = 'RELEASED'
                    """, applicationId.toString(), Timestamp.from(now), type.name(), value) == 1;
        }
        return false;
    }

    @Override
    public void releaseByApplicationId(UUID applicationId, Instant now) {
        jdbcTemplate.update("UPDATE onboarding_uniqueness_reservation SET status = 'RELEASED', updated_at = ? WHERE application_id = ? AND status = 'ACTIVE'",
                Timestamp.from(now), applicationId.toString());
    }

    @Override
    public void convertByApplicationId(UUID applicationId, Instant now) {
        jdbcTemplate.update("UPDATE onboarding_uniqueness_reservation SET status = 'CONVERTED', updated_at = ? WHERE application_id = ? AND status = 'ACTIVE'",
                Timestamp.from(now), applicationId.toString());
    }
}
