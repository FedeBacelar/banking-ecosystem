package com.fedebacelar.bank.notification.infrastructure.config;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    private static final int MAX_TIMEOUT_MILLIS = 60_000;

    @Bean
    JavaMailSender javaMailSender(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean auth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTls,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean startTlsRequired,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}") boolean sslEnabled,
            @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}") int connectionTimeout,
            @Value("${spring.mail.properties.mail.smtp.timeout:10000}") int readTimeout,
            @Value("${spring.mail.properties.mail.smtp.writetimeout:10000}") int writeTimeout
    ) {
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasPassword = password != null && !password.isBlank();
        if (hasUsername != hasPassword) {
            throw new IllegalStateException("SMTP username and password must be configured together.");
        }

        if (startTlsRequired && !startTls) {
            throw new IllegalStateException("SMTP STARTTLS must be enabled when it is required.");
        }

        if (sslEnabled && (startTls || startTlsRequired)) {
            throw new IllegalStateException(
                    "SMTP implicit SSL and STARTTLS cannot be enabled together."
            );
        }

        boolean hasCredentials = hasUsername && hasPassword;
        if ((auth || hasCredentials) && !startTlsRequired && !sslEnabled) {
            throw new IllegalStateException(
                    "Authenticated SMTP requires mandatory STARTTLS or implicit SSL."
            );
        }

        if (auth != hasCredentials) {
            throw new IllegalStateException(
                    "SMTP authentication and credentials must be enabled together."
            );
        }

        requireBoundedTimeout("connection", connectionTimeout);
        requireBoundedTimeout("read", readTimeout);
        requireBoundedTimeout("write", writeTimeout);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (hasCredentials) {
            sender.setUsername(username);
            sender.setPassword(password);
        }

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", Boolean.toString(auth));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        properties.put("mail.smtp.starttls.required", Boolean.toString(startTlsRequired));
        properties.put("mail.smtp.ssl.enable", Boolean.toString(sslEnabled));
        properties.put(
                "mail.smtp.ssl.checkserveridentity",
                Boolean.toString(startTlsRequired || sslEnabled)
        );
        properties.put("mail.smtp.connectiontimeout", Integer.toString(connectionTimeout));
        properties.put("mail.smtp.timeout", Integer.toString(readTimeout));
        properties.put("mail.smtp.writetimeout", Integer.toString(writeTimeout));

        return sender;
    }

    private static void requireBoundedTimeout(String name, int timeout) {
        if (timeout < 1 || timeout > MAX_TIMEOUT_MILLIS) {
            throw new IllegalStateException(
                    "SMTP " + name + " timeout is outside the allowed range."
            );
        }
    }
}

