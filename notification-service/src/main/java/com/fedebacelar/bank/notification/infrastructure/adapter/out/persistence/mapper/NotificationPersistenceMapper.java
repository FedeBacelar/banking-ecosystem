package com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceMapper {

    private static final TypeReference<Map<String, String>> VARIABLES_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public NotificationPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationEntity toEntity(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(notification.id().toString());
        entity.setChannel(notification.channel());
        entity.setRecipient(notification.recipient());
        entity.setTemplateCode(notification.templateCode());
        entity.setVariablesJson(toJson(notification.variables()));
        entity.setCorrelationId(notification.correlationId());
        entity.setRequestFingerprint(notification.requestFingerprint());
        entity.setSubject(notification.subject());
        entity.setBody(notification.body());
        entity.setHtmlBody(notification.htmlBody());
        entity.setStatus(notification.status());
        entity.setAttemptCount(notification.attemptCount());
        entity.setLastError(notification.lastError());
        entity.setSentAt(notification.sentAt());
        entity.setCreatedAt(notification.createdAt());
        entity.setUpdatedAt(notification.updatedAt());
        entity.setVersion(notification.version());
        return entity;
    }

    public Notification toDomain(NotificationEntity entity) {
        return new Notification(
                UUID.fromString(entity.getId()),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getTemplateCode(),
                fromJson(entity.getVariablesJson()),
                entity.getCorrelationId(),
                entity.getRequestFingerprint(),
                entity.getSubject(),
                entity.getBody(),
                entity.getHtmlBody(),
                entity.getStatus(),
                entity.getAttemptCount(),
                entity.getLastError(),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private String toJson(Map<String, String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Map<String, String> fromJson(String variablesJson) {
        try {
            return objectMapper.readValue(variablesJson, VARIABLES_TYPE);
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
