package com.fedebacelar.bank.notification.infrastructure.adapter.out.smtp;

import com.fedebacelar.bank.notification.application.port.out.EmailDeliveryPort;
import com.fedebacelar.bank.notification.domain.exception.EmailDeliveryException;
import com.fedebacelar.bank.notification.domain.model.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailDeliveryAdapter implements EmailDeliveryPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailDeliveryAdapter(
            JavaMailSender mailSender,
            @Value("${notification.email.from}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void deliver(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(notification.recipient());
        message.setSubject(notification.subject());
        message.setText(notification.body());

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailDeliveryException("Email delivery failed: " + exception.getMessage(), exception);
        }
    }
}

