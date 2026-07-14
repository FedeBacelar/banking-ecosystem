package com.fedebacelar.bank.notification.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.port.in.SendEmailNotificationUseCase;
import com.fedebacelar.bank.notification.application.view.NotificationDetails;
import com.fedebacelar.bank.notification.domain.enums.NotificationChannel;
import com.fedebacelar.bank.notification.domain.enums.NotificationStatus;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.InvalidTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.MissingTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.NotificationRequestConflictException;
import com.fedebacelar.bank.notification.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.notification.infrastructure.adapter.in.web.mapper.NotificationWebMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = NotificationController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({NotificationWebMapper.class, GlobalExceptionHandler.class})
class NotificationWebAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SendEmailNotificationUseCase sendEmailNotificationUseCase;

    @Test
    void sendsEmailNotification() throws Exception {
        when(sendEmailNotificationUseCase.send(any())).thenReturn(notificationDetails(NotificationStatus.SENT));

        mockMvc.perform(post("/internal/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "person@example.com",
                                  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
                                  "variables": {
                                    "magicLink": "http://localhost:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "expiresInMinutes": "30"
                                  },
                                  "correlationId": "application-1",
                                  "sensitive": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.templateCode").value("ONBOARDING_EMAIL_MAGIC_LINK"));
    }

    @Test
    void treatsAnOmittedLegacySensitiveFlagAsFalseWithoutDowngradingTemplatePolicy() throws Exception {
        when(sendEmailNotificationUseCase.send(any())).thenReturn(notificationDetails(NotificationStatus.SENT));

        mockMvc.perform(post("/internal/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "person@example.com",
                                  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
                                  "variables": {
                                    "magicLink": "http://localhost:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "expiresInMinutes": "30"
                                  },
                                  "correlationId": "application-without-legacy-flag"
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<SendEmailNotificationCommand> command =
                ArgumentCaptor.forClass(SendEmailNotificationCommand.class);
        verify(sendEmailNotificationUseCase).send(command.capture());
        assertThat(command.getValue().sensitive()).isFalse();
    }

    @Test
    void returnsBadRequestForInvalidEmail() throws Exception {
        mockMvc.perform(post("/internal/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "not-an-email",
                                  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
                                  "variables": {
                                    "magicLink": "http://localhost",
                                    "expiresInMinutes": "30"
                                  },
                                  "sensitive": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsBadRequestForMissingTemplateVariable() throws Exception {
        when(sendEmailNotificationUseCase.send(any()))
                .thenThrow(new MissingTemplateVariableException(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, "expiresInMinutes"));

        mockMvc.perform(post("/internal/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "person@example.com",
                                  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
                                  "variables": {
                                    "magicLink": "http://localhost"
                                  },
                                  "sensitive": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing template variable"));
    }

    @Test
    void returnsBadRequestForUnsafeTemplateVariableWithoutEchoingItsValue() throws Exception {
        when(sendEmailNotificationUseCase.send(any()))
                .thenThrow(new InvalidTemplateVariableException(
                        NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                        "magicLink"
                ));

        mockMvc.perform(post("/internal/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "person@example.com",
                                  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
                                  "variables": {
                                    "magicLink": "invalid-value",
                                    "expiresInMinutes": "30"
                                  },
                                  "sensitive": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid template variable"))
                .andExpect(jsonPath("$.detail").value("Invalid variable 'magicLink' for template ONBOARDING_EMAIL_MAGIC_LINK"));
    }

    @Test
    void returnsConflictWithoutReflectingRequestPayloadForIdempotencyMismatch() throws Exception {
        when(sendEmailNotificationUseCase.send(any()))
                .thenThrow(new NotificationRequestConflictException());

        mockMvc.perform(post("/internal/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "person@example.com",
                                  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
                                  "variables": {
                                    "magicLink": "http://localhost:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "expiresInMinutes": "30"
                                  },
                                  "correlationId": "application-1",
                                  "sensitive": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Notification request conflict"))
                .andExpect(jsonPath("$.detail").value(
                        "Notification idempotency key is already associated with a different request"
                ));
    }

    private NotificationDetails notificationDetails(NotificationStatus status) {
        Instant now = Instant.parse("2026-07-04T21:00:00Z");
        return new NotificationDetails(
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "application-1",
                "Subject",
                status,
                1,
                null,
                now,
                now,
                now
        );
    }
}

