package com.fedebacelar.bank.notification.infrastructure.adapter.out.smtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailDeliveryAdapterTest {

    @Test
    void sendsHtmlAndPlainTextAsMultipartAlternativeWithoutUsingAnSmtpServer() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        SmtpEmailDeliveryAdapter adapter = new SmtpEmailDeliveryAdapter(
                mailSender,
                "no-reply@nerva.local",
                "Nerva Banking"
        );
        Notification notification = Notification.createEmail(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of(),
                "delivery-example",
                new RenderedNotification(
                        "Continuá tu solicitud",
                        "Texto para clientes sin HTML.",
                        "<html><body><h1>Continuá tu solicitud</h1></body></html>"
                ),
                Instant.parse("2026-07-13T12:00:00Z")
        );

        adapter.deliver(notification);
        message.saveChanges();

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Continuá tu solicitud");
        assertThat(message.getFrom()).extracting(Object::toString)
                .containsExactly("Nerva Banking <no-reply@nerva.local>");
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("person@example.com");
        assertThat(containsMimeType(message, "multipart/alternative")).isTrue();
        assertThat(findContent(message, "text/plain")).isEqualTo("Texto para clientes sin HTML.");
        assertThat(findContent(message, "text/html"))
                .isEqualTo("<html><body><h1>Continuá tu solicitud</h1></body></html>");
    }

    private String findContent(Part part, String contentType) throws Exception {
        if (part.isMimeType(contentType)) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                String content = findContent(bodyPart, contentType);
                if (content != null) {
                    return content;
                }
            }
        }
        return null;
    }

    private boolean containsMimeType(Part part, String contentType) throws Exception {
        if (part.isMimeType(contentType)) {
            return true;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                if (containsMimeType(multipart.getBodyPart(index), contentType)) {
                    return true;
                }
            }
        }
        return false;
    }
}
