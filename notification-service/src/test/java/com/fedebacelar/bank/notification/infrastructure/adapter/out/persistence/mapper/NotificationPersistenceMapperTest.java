package com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationPersistenceMapperTest {

    @Test
    void preservesRequestFingerprintAcrossPersistenceMapping() {
        NotificationPersistenceMapper mapper = new NotificationPersistenceMapper(new ObjectMapper());
        Notification notification = Notification.createEmail(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of("magicLink", "redacted-before-persistence"),
                "delivery-1",
                "a".repeat(64),
                new RenderedNotification("Subject", "Body", "<p>Body</p>"),
                Instant.parse("2026-07-14T00:00:00Z")
        ).redactContent();

        var entity = mapper.toEntity(notification);
        var restored = mapper.toDomain(entity);

        assertThat(entity.getRequestFingerprint()).isEqualTo("a".repeat(64));
        assertThat(restored.requestFingerprint()).isEqualTo("a".repeat(64));
        assertThat(restored.contentRedacted()).isTrue();
    }
}
