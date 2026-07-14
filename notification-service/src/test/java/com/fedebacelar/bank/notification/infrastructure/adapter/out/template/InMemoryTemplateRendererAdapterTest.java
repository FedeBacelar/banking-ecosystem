package com.fedebacelar.bank.notification.infrastructure.adapter.out.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.InvalidTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.MissingTemplateVariableException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryTemplateRendererAdapterTest {

    private static final String MAGIC_TOKEN = "A".repeat(43);
    private static final String KEYCLOAK_ACTION_KEY = "header.payload.signature";
    private final InMemoryTemplateRendererAdapter renderer = new InMemoryTemplateRendererAdapter(
            new EmailActionLinkPolicy(
                    "http://localhost:4200,https://nerva.example",
                    "http://localhost:8090,https://identity.nerva.example"
            )
    );

    @Test
    void rendersMagicLinkAsBrandedHtmlAndPlainText() {
        var rendered = renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of(
                "magicLink", "https://nerva.example/onboarding/continue#token=" + MAGIC_TOKEN,
                "expiresInMinutes", "30"
        ));

        assertThat(rendered.subject()).isEqualTo("Continuá tu solicitud en Nerva Banking");
        assertThat(rendered.textBody())
                .contains("Confirmá tu correo")
                .contains("https://nerva.example/onboarding/continue#token=" + MAGIC_TOKEN)
                .contains("30 minutos")
                .contains("Proyecto académico")
                .contains("datos y documentos de prueba");
        assertThat(rendered.htmlBody())
                .startsWith("<!doctype html>")
                .contains("<html lang=\"es\">")
                .contains("Nerva Banking")
                .contains("Continuar solicitud")
                .contains("href=\"https://nerva.example/onboarding/continue#token=" + MAGIC_TOKEN + "\"")
                .contains("Proyecto académico")
                .doesNotContain("{{");
    }

    @Test
    void providesConsistentHtmlAndPlainTextForEveryNotificationTemplate() {
        Map<NotificationTemplateCode, Map<String, String>> variables = Map.of(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of(
                        "magicLink", "http://localhost:4200/onboarding/continue#token=" + MAGIC_TOKEN,
                        "expiresInMinutes", "30"
                ),
                NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                Map.of(
                        "firstName", "Federico",
                        "credentialSetupLink", "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token?key=" + KEYCLOAK_ACTION_KEY,
                        "expiresInHours", "12"
                ),
                NotificationTemplateCode.ONBOARDING_REJECTED,
                Map.of("firstName", "Federico"),
                NotificationTemplateCode.ONBOARDING_COMPLETED,
                Map.of("firstName", "Federico")
        );

        variables.forEach((code, templateVariables) -> {
            var rendered = renderer.render(code, templateVariables);

            assertThat(rendered.subject()).isNotBlank();
            assertThat(rendered.textBody())
                    .contains("Nerva Banking")
                    .contains("Proyecto académico")
                    .doesNotContain("{{");
            assertThat(rendered.htmlBody())
                    .contains("<!doctype html>")
                    .contains("BANCA DIGITAL")
                    .contains("Proyecto académico")
                    .doesNotContain("{{");
        });
    }

    @Test
    void escapesCustomerContentInHtmlWithoutChangingThePlainTextAlternative() {
        var rendered = renderer.render(
                NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                Map.of(
                        "firstName", "O'Connor",
                        "credentialSetupLink", "https://identity.nerva.example/realms/banking-ecosystem/login-actions/action-token?key=" + KEYCLOAK_ACTION_KEY,
                        "expiresInHours", "12"
                )
        );

        assertThat(rendered.textBody()).contains("O'Connor");
        assertThat(rendered.htmlBody())
                .contains("O&#39;Connor")
                .contains("action-token?key=" + KEYCLOAK_ACTION_KEY)
                .doesNotContain("O'Connor");
    }

    @Test
    void rejectsMissingVariables() {
        assertThatThrownBy(() -> renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of(
                "magicLink", "http://localhost:4200/onboarding/continue#token=" + MAGIC_TOKEN
        )))
                .isInstanceOf(MissingTemplateVariableException.class)
                .hasMessageContaining("expiresInMinutes");
    }

    @Test
    void rejectsUnsafeActionLinksWithoutIncludingTheirValueInTheError() {
        String unsafeLink = "javascript:alert(document.cookie)";

        assertThatThrownBy(() -> renderer.render(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of("magicLink", unsafeLink, "expiresInMinutes", "30")
        ))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessageContaining("magicLink")
                .hasMessageNotContaining(unsafeLink);
    }

    @Test
    void rejectsUnencryptedNonLocalActionLinks() {
        assertThatThrownBy(() -> renderer.render(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of("magicLink", "http://nerva.example/onboarding/continue", "expiresInMinutes", "30")
        ))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessageContaining("magicLink");
    }

    @Test
    void rejectsHttpsLinksOutsideTheConfiguredTemplateOrigin() {
        assertThatThrownBy(() -> renderer.render(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of(
                        "magicLink", "https://attacker.example/onboarding/continue#token=" + MAGIC_TOKEN,
                        "expiresInMinutes", "30"
                )
        ))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessageContaining("magicLink")
                .hasMessageNotContaining("attacker.example");
    }

    @Test
    void rejectsVariablesOutsideTheExactTemplateContract() {
        assertThatThrownBy(() -> renderer.render(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of(
                        "magicLink", "http://localhost:4200/onboarding/continue#token=" + MAGIC_TOKEN,
                        "expiresInMinutes", "30",
                        "hiddenSecret", "must-not-be-persisted"
                )
        ))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessageContaining("variables")
                .hasMessageNotContaining("hiddenSecret")
                .hasMessageNotContaining("must-not-be-persisted");
    }

    @Test
    void acceptsHumanFirstNamesWithUnicodeMarksAndExpectedPunctuation() {
        List.of(
                "Federico",
                "María José",
                "María\u00a0José",
                "A\u0301na",
                "O'Connor",
                "D’Angelo",
                "Jean-Luc",
                "J.",
                "A".repeat(80)
        ).forEach(firstName -> {
            var rendered = renderer.render(
                    NotificationTemplateCode.ONBOARDING_REJECTED,
                    Map.of("firstName", firstName)
            );

            assertThat(rendered.textBody()).contains(firstName);
        });
    }

    @Test
    void rejectsControlsNewlinesUntrimmedNamesAndDeceptiveCopy() {
        List.of(
                "Federico\nIngresá ahora",
                "Federico\rIngresá ahora",
                "Federico\tBacelar",
                "Federico\u0000Bacelar",
                "Federico\u001bBacelar",
                "Federico\u007fBacelar",
                "Federico\u202eIngresá ahora",
                "Federico\u2028Ingresá ahora",
                "Federico\u2029Ingresá ahora",
                " Federico",
                "Federico ",
                "\u00a0Federico",
                "Federico\u00a0",
                "A".repeat(81),
                "...",
                "-Federico",
                "Federico-",
                "'Federico",
                "URGENTE: ingresá ahora",
                "Ingresá en https://attacker.example",
                "Federico | verificá tu cuenta",
                "<b>Federico</b>"
        ).forEach(this::assertInvalidFirstName);
    }

    @Test
    void acceptsExpirationBoundsForMinutesAndHours() {
        for (String minutes : List.of("1", "1440")) {
            assertThat(renderer.render(
                    NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                    Map.of(
                            "magicLink", "http://localhost:4200/onboarding/continue#token=" + MAGIC_TOKEN,
                            "expiresInMinutes", minutes
                    )
            ).textBody()).contains(minutes + " minutos");
        }

        for (String hours : List.of("1", "168")) {
            assertThat(renderer.render(
                    NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                    Map.of(
                            "firstName", "Federico",
                            "credentialSetupLink", "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token?key=" + KEYCLOAK_ACTION_KEY,
                            "expiresInHours", hours
                    )
            ).textBody()).contains(hours + " horas");
        }
    }

    @Test
    void rejectsInvalidMinuteValuesWithoutReflectingThem() {
        List.of(
                "0", "-1", "+1", "1.5", "1e3", " 30", "30 ", "030", "1441",
                "2147483648", "treinta", "30\nRevisá tu cuenta"
        ).forEach(value -> assertInvalidExpiration(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "expiresInMinutes",
                value
        ));
    }

    @Test
    void rejectsInvalidHourValuesWithoutReflectingThem() {
        List.of(
                "0", "-1", "+1", "12.5", "1e2", " 12", "12 ", "012", "169",
                "2147483648", "doce", "12\r\nIngresá ahora"
        ).forEach(value -> assertInvalidExpiration(
                NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                "expiresInHours",
                value
        ));
    }

    private void assertInvalidFirstName(String value) {
        assertThatThrownBy(() -> renderer.render(
                NotificationTemplateCode.ONBOARDING_REJECTED,
                Map.of("firstName", value)
        ))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessage("Invalid variable 'firstName' for template ONBOARDING_REJECTED");
    }

    private void assertInvalidExpiration(
            NotificationTemplateCode templateCode,
            String variableName,
            String value
    ) {
        Map<String, String> variables = templateCode == NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK
                ? Map.of(
                        "magicLink", "http://localhost:4200/onboarding/continue#token=" + MAGIC_TOKEN,
                        variableName, value
                )
                : Map.of(
                        "firstName", "Federico",
                        "credentialSetupLink", "http://localhost:8090/realms/banking-ecosystem/login-actions/action-token?key=" + KEYCLOAK_ACTION_KEY,
                        variableName, value
                );

        assertThatThrownBy(() -> renderer.render(templateCode, variables))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessage("Invalid variable '" + variableName + "' for template " + templateCode)
                .hasMessageNotContaining(value);
    }
}
