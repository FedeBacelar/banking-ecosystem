package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingEmailRequestGuardPort;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OnboardingEmailRequestGuardAdapter implements OnboardingEmailRequestGuardPort {

    private final JdbcTemplate jdbcTemplate;

    public OnboardingEmailRequestGuardAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean acquireAndRegister(String email, Instant now, Duration cooldown) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO onboarding_email_request_guard "
                        + "(email, last_requested_at, created_at, updated_at) VALUES (?, NULL, ?, ?)",
                email, Timestamp.from(now), Timestamp.from(now)
        );
        Instant lastRequestedAt = jdbcTemplate.queryForObject(
                "SELECT last_requested_at FROM onboarding_email_request_guard WHERE email = ? FOR UPDATE",
                (resultSet, row) -> {
                    Timestamp value = resultSet.getTimestamp(1);
                    return value == null ? null : value.toInstant();
                },
                email
        );
        if (lastRequestedAt != null && now.isBefore(lastRequestedAt.plus(cooldown))) {
            return false;
        }
        jdbcTemplate.update(
                "UPDATE onboarding_email_request_guard SET last_requested_at = ?, updated_at = ? WHERE email = ?",
                Timestamp.from(now), Timestamp.from(now), email
        );
        return true;
    }
}
