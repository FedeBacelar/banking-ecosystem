package com.fedebacelar.bank.notification.infrastructure.adapter.out.template;

import com.fedebacelar.bank.notification.application.port.out.TemplateRendererPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.MissingTemplateVariableException;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class InMemoryTemplateRendererAdapter implements TemplateRendererPort {

    private static final Map<NotificationTemplateCode, TemplateDefinition> TEMPLATES = Map.of(
            NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
            new TemplateDefinition(
                    "Continuá tu solicitud en Nerva Banking",
                    """
                            Hola,

                            Usá este enlace para confirmar tu correo y completar la solicitud:

                            {{magicLink}}

                            El enlace vence en {{expiresInMinutes}} minutos.

                            Si no pediste este correo, podés ignorarlo.

                            Nerva Banking es un proyecto académico: no abre una cuenta real.
                            Usá únicamente datos y documentos de prueba.
                            """,
                    Set.of("magicLink", "expiresInMinutes")
            ),
            NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
            new TemplateDefinition(
                    "Tu solicitud fue aprobada",
                    """
                            Hola {{firstName}},

                            Tu solicitud fue aprobada. Para crear tus credenciales de home banking, usa este enlace:

                            {{credentialSetupLink}}

                            El enlace vence en {{expiresInHours}} horas.
                            """,
                    Set.of("firstName", "credentialSetupLink", "expiresInHours")
            ),
            NotificationTemplateCode.ONBOARDING_REJECTED,
            new TemplateDefinition(
                    "No pudimos aprobar tu solicitud",
                    """
                            Hola {{firstName}},

                            Revisamos tu solicitud y no pudimos aprobarla en este momento.

                            Motivo: {{reason}}
                            """,
                    Set.of("firstName", "reason")
            ),
            NotificationTemplateCode.ONBOARDING_COMPLETED,
            new TemplateDefinition(
                    "Tu acceso a Nerva Banking esta listo",
                    """
                            Hola {{firstName}},

                            Tu alta fue completada y tu acceso a home banking ya esta disponible.
                            """,
                    Set.of("firstName")
            )
    );

    @Override
    public RenderedNotification render(NotificationTemplateCode templateCode, Map<String, String> variables) {
        TemplateDefinition template = TEMPLATES.get(templateCode);
        for (String requiredVariable : template.requiredVariables()) {
            if (!variables.containsKey(requiredVariable) || variables.get(requiredVariable) == null || variables.get(requiredVariable).isBlank()) {
                throw new MissingTemplateVariableException(templateCode, requiredVariable);
            }
        }

        String body = template.body();
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            body = body.replace("{{" + variable.getKey() + "}}", variable.getValue());
        }

        return new RenderedNotification(template.subject(), body);
    }

    private record TemplateDefinition(
            String subject,
            String body,
            Set<String> requiredVariables
    ) {
    }
}

