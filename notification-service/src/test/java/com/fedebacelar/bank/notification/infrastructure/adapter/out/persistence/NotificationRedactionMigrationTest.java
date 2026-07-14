package com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class NotificationRedactionMigrationTest {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(
            DockerImageName.parse("mysql:8.4")
    );

    @Test
    void migrationScrubsEverySensitiveTemplateAndLeavesLegacyFingerprintNull() throws Exception {
        flyway(MigrationVersion.fromVersion("3")).migrate();
        insertLegacySensitiveRows();

        flyway(null).migrate();

        Set<NotificationTemplateCode> remaining = EnumSet.allOf(NotificationTemplateCode.class);
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()
        ); var statement = connection.createStatement(); var rows = statement.executeQuery("""
                SELECT template_code, variables_json, body, html_body, request_fingerprint
                FROM notification
                ORDER BY template_code
                """)) {
            int count = 0;
            while (rows.next()) {
                count++;
                remaining.remove(NotificationTemplateCode.valueOf(rows.getString("template_code")));
                assertThat(rows.getString("variables_json")).isEqualTo("{}");
                assertThat(rows.getString("body")).isEqualTo("[REDACTED]");
                assertThat(rows.getString("html_body")).isEqualTo("[REDACTED]");
                assertThat(rows.getString("request_fingerprint")).isNull();
            }
            assertThat(count).isEqualTo(NotificationTemplateCode.values().length);
            assertThat(remaining).isEmpty();
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void insertLegacySensitiveRows() throws Exception {
        String sql = """
                INSERT INTO notification (
                    id, channel, recipient, template_code, variables_json,
                    correlation_id, subject, body, html_body, status,
                    attempt_count, sent_at, created_at, updated_at
                ) VALUES (?, 'EMAIL', 'legacy@example.com', ?, ?, ?, 'Subject', ?, ?,
                          'SENT', 1, ?, ?, ?)
                """;
        Instant now = Instant.parse("2026-07-14T00:00:00Z");
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()
        ); var statement = connection.prepareStatement(sql)) {
            for (NotificationTemplateCode templateCode : NotificationTemplateCode.values()) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setString(2, templateCode.name());
                statement.setString(3, "{\"secret\":\"legacy-token\"}");
                statement.setString(4, "legacy-" + templateCode.name());
                statement.setString(5, "Legacy body with token");
                statement.setString(6, "<p>Legacy body with token</p>");
                statement.setTimestamp(7, Timestamp.from(now));
                statement.setTimestamp(8, Timestamp.from(now));
                statement.setTimestamp(9, Timestamp.from(now));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
