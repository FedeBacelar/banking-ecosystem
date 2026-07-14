package com.fedebacelar.bank.notification.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MailConfig.class)
            .withPropertyValues(
                    "spring.mail.host=localhost",
                    "spring.mail.port=1025"
            );

    @Test
    void createsMailpitSenderWithSafeDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            var sender = context.getBean(JavaMailSenderImpl.class);
            assertThat(sender.getHost()).isEqualTo("localhost");
            assertThat(sender.getPort()).isEqualTo(1025);
            assertThat(sender.getUsername()).isNull();
            assertThat(sender.getPassword()).isNull();
            assertThat(sender.getJavaMailProperties())
                    .containsEntry("mail.smtp.auth", "false")
                    .containsEntry("mail.smtp.starttls.enable", "false")
                    .containsEntry("mail.smtp.starttls.required", "false")
                    .containsEntry("mail.smtp.ssl.enable", "false")
                    .containsEntry("mail.smtp.ssl.checkserveridentity", "false")
                    .containsEntry("mail.smtp.connectiontimeout", "5000")
                    .containsEntry("mail.smtp.timeout", "10000")
                    .containsEntry("mail.smtp.writetimeout", "10000");
        });
    }

    @Test
    void createsAuthenticatedSenderWithMandatoryStartTlsAndBoundedTimeouts() {
        contextRunner
                .withPropertyValues(
                        "spring.mail.username=portfolio-user",
                        "spring.mail.password=portfolio-password",
                        "spring.mail.properties.mail.smtp.auth=true",
                        "spring.mail.properties.mail.smtp.starttls.enable=true",
                        "spring.mail.properties.mail.smtp.starttls.required=true",
                        "spring.mail.properties.mail.smtp.connectiontimeout=3000",
                        "spring.mail.properties.mail.smtp.timeout=7000",
                        "spring.mail.properties.mail.smtp.writetimeout=8000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    var sender = context.getBean(JavaMailSenderImpl.class);
                    assertThat(sender.getUsername()).isEqualTo("portfolio-user");
                    assertThat(sender.getPassword()).isEqualTo("portfolio-password");
                    assertThat(sender.getJavaMailProperties())
                            .containsEntry("mail.smtp.auth", "true")
                            .containsEntry("mail.smtp.starttls.enable", "true")
                            .containsEntry("mail.smtp.starttls.required", "true")
                            .containsEntry("mail.smtp.ssl.enable", "false")
                            .containsEntry("mail.smtp.ssl.checkserveridentity", "true")
                            .containsEntry("mail.smtp.connectiontimeout", "3000")
                            .containsEntry("mail.smtp.timeout", "7000")
                            .containsEntry("mail.smtp.writetimeout", "8000");
                });
    }

    @Test
    void createsAuthenticatedSenderWithImplicitSsl() {
        contextRunner
                .withPropertyValues(
                        "spring.mail.username=portfolio-user",
                        "spring.mail.password=portfolio-password",
                        "spring.mail.properties.mail.smtp.auth=true",
                        "spring.mail.properties.mail.smtp.ssl.enable=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JavaMailSenderImpl.class).getJavaMailProperties())
                            .containsEntry("mail.smtp.starttls.required", "false")
                            .containsEntry("mail.smtp.ssl.enable", "true")
                            .containsEntry("mail.smtp.ssl.checkserveridentity", "true");
                });
    }

    @ParameterizedTest(name = "rejects invalid SMTP configuration: {0}")
    @MethodSource("invalidConfigurations")
    void rejectsInvalidSmtpConfiguration(
            String scenario,
            String expectedMessage,
            String[] properties
    ) {
        contextRunner
                .withPropertyValues(properties)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasStackTraceContaining(expectedMessage);
                });
    }

    private static Stream<Arguments> invalidConfigurations() {
        return Stream.of(
                Arguments.of(
                        "username without password",
                        "SMTP username and password must be configured together.",
                        new String[]{"spring.mail.username=portfolio-user"}
                ),
                Arguments.of(
                        "password without username",
                        "SMTP username and password must be configured together.",
                        new String[]{"spring.mail.password=portfolio-password"}
                ),
                Arguments.of(
                        "STARTTLS required but disabled",
                        "SMTP STARTTLS must be enabled when it is required.",
                        new String[]{"spring.mail.properties.mail.smtp.starttls.required=true"}
                ),
                Arguments.of(
                        "implicit SSL together with STARTTLS",
                        "SMTP implicit SSL and STARTTLS cannot be enabled together.",
                        new String[]{
                                "spring.mail.properties.mail.smtp.ssl.enable=true",
                                "spring.mail.properties.mail.smtp.starttls.enable=true"
                        }
                ),
                Arguments.of(
                        "authentication over optional STARTTLS",
                        "Authenticated SMTP requires mandatory STARTTLS or implicit SSL.",
                        new String[]{
                                "spring.mail.username=portfolio-user",
                                "spring.mail.password=portfolio-password",
                                "spring.mail.properties.mail.smtp.auth=true",
                                "spring.mail.properties.mail.smtp.starttls.enable=true"
                        }
                ),
                Arguments.of(
                        "authentication without credentials",
                        "SMTP authentication and credentials must be enabled together.",
                        new String[]{
                                "spring.mail.properties.mail.smtp.auth=true",
                                "spring.mail.properties.mail.smtp.ssl.enable=true"
                        }
                ),
                Arguments.of(
                        "credentials while authentication is disabled",
                        "SMTP authentication and credentials must be enabled together.",
                        new String[]{
                                "spring.mail.username=portfolio-user",
                                "spring.mail.password=portfolio-password",
                                "spring.mail.properties.mail.smtp.ssl.enable=true"
                        }
                ),
                Arguments.of(
                        "unbounded connection timeout",
                        "SMTP connection timeout is outside the allowed range.",
                        new String[]{"spring.mail.properties.mail.smtp.connectiontimeout=0"}
                ),
                Arguments.of(
                        "unbounded read timeout",
                        "SMTP read timeout is outside the allowed range.",
                        new String[]{"spring.mail.properties.mail.smtp.timeout=60001"}
                ),
                Arguments.of(
                        "unbounded write timeout",
                        "SMTP write timeout is outside the allowed range.",
                        new String[]{"spring.mail.properties.mail.smtp.writetimeout=-1"}
                )
        );
    }
}
