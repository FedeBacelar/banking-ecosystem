package com.fedebacelar.bank.notification.infrastructure.adapter.out.smtp;

import com.fedebacelar.bank.notification.application.port.out.EmailDeliveryPort;
import com.fedebacelar.bank.notification.domain.exception.EmailDeliveryException;
import com.fedebacelar.bank.notification.domain.model.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailDeliveryAdapter implements EmailDeliveryPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmailDeliveryAdapter(
            JavaMailSender mailSender,
            @Value("${notification.email.from}") String fromAddress,
            @Value("${notification.email.from-name:Nerva Banking}") String fromName
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void deliver(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.recipient());
            helper.setSubject(notification.subject());
            if (notification.htmlBody() == null || notification.htmlBody().isBlank()) {
                helper.setText(notification.body());
            } else {
                helper.setText(notification.body(), notification.htmlBody());
            }
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException exception) {
            throw new EmailDeliveryException("Email delivery failed: " + exception.getMessage(), exception);
        }
    }
}

