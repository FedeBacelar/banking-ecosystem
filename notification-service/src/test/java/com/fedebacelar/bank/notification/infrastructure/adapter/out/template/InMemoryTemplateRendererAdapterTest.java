package com.fedebacelar.bank.notification.infrastructure.adapter.out.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.InvalidTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.MissingTemplateVariableException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryTemplateRendererAdapterTest {

    private final InMemoryTemplateRendererAdapter renderer = new InMemoryTemplateRendererAdapter();

    @Test
    void rendersMagicLinkAsBrandedHtmlAndPlainText() {
        var rendered = renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of(
                "magicLink", "https://nerva.example/onboarding/continue?request=example&lang=es",
                "expiresInMinutes", "30"
        ));

        assertThat(rendered.subject()).isEqualTo("Continuá tu solicitud en Nerva Banking");
        assertThat(rendered.textBody())
                .contains("Confirmá tu correo")
                .contains("https://nerva.example/onboarding/continue?request=example&lang=es")
                .contains("30 minutos")
                .contains("Proyecto académico")
                .contains("datos y documentos de prueba");
        assertThat(rendered.htmlBody())
                .startsWith("<!doctype html>")
                .contains("<html lang=\"es\">")
                .contains("Nerva Banking")
                .contains("Continuar solicitud")
                .contains("href=\"https://nerva.example/onboarding/continue?request=example&amp;lang=es\"")
                .contains("Proyecto académico")
                .doesNotContain("{{");
    }

    @Test
    void providesConsistentHtmlAndPlainTextForEveryNotificationTemplate() {
        Map<NotificationTemplateCode, Map<String, String>> variables = Map.of(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of("magicLink", "http://localhost:4200/onboarding/continue", "expiresInMinutes", "30"),
                NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
                Map.of(
                        "firstName", "Federico",
                        "credentialSetupLink", "http://127.0.0.1:8090/realms/banking/login-actions/action-token",
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
                        "firstName", "Fede <Bacelar> & familia",
                        "credentialSetupLink", "https://identity.nerva.example/setup?request=a&source=email",
                        "expiresInHours", "12"
                )
        );

        assertThat(rendered.textBody()).contains("Fede <Bacelar> & familia");
        assertThat(rendered.htmlBody())
                .contains("Fede &lt;Bacelar&gt; &amp; familia")
                .contains("setup?request=a&amp;source=email")
                .doesNotContain("Fede <Bacelar>");
    }

    @Test
    void rejectsMissingVariables() {
        assertThatThrownBy(() -> renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of(
                "magicLink", "http://localhost:4200/onboarding/continue"
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
}
