package com.fedebacelar.bank.notification.infrastructure.adapter.out.template;

import com.fedebacelar.bank.notification.application.port.out.TemplateRendererPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.InvalidTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.MissingTemplateVariableException;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class InMemoryTemplateRendererAdapter implements TemplateRendererPort {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9]*)}}");
    private static final Pattern HUMAN_FIRST_NAME_PATTERN = Pattern.compile(
            "[\\p{L}\\p{M}](?:[\\p{L}\\p{M}\\p{Zs}.'’-]*[\\p{L}\\p{M}.])?"
    );
    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("[1-9][0-9]*");
    private static final int MAX_FIRST_NAME_CODE_POINTS = 80;
    private static final int MAX_EXPIRATION_MINUTES = 1_440;
    private static final int MAX_EXPIRATION_HOURS = 168;
    private static final String ACADEMIC_NOTICE_TEXT = """
            Proyecto académico
            Nerva Banking no es una entidad financiera ni abre cuentas reales. Usá únicamente datos y documentos de prueba.
            """.strip();
    private static final String ACTION_HTML = """
            <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:28px 0 24px;">
              <tr>
                <td align="center" style="border-radius:9px;background:#173b6c;">
                  <a href="{{actionUrl}}" style="display:inline-block;padding:14px 22px;color:#ffffff;font-size:15px;font-weight:700;line-height:20px;text-decoration:none;">{{actionLabel}}</a>
                </td>
              </tr>
            </table>
            """;
    private static final String HTML_LAYOUT = """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="color-scheme" content="light only">
              <title>__SUBJECT__</title>
              <style>
                @media only screen and (max-width: 620px) {
                  .email-shell { padding: 24px 12px !important; }
                  .email-card { padding: 30px 24px !important; }
                }
              </style>
            </head>
            <body style="margin:0;padding:0;background:#f4f7fb;color:#101828;font-family:Arial,'Helvetica Neue',sans-serif;">
              <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">__PREHEADER__</div>
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;background:#f4f7fb;">
                <tr>
                  <td class="email-shell" align="center" style="padding:32px 16px;">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:600px;">
                      <tr>
                        <td style="padding:0 0 18px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td width="38" height="38" align="center" valign="middle" style="width:38px;height:38px;border-radius:10px;background:#173b6c;color:#38bdf8;font-size:22px;font-weight:700;line-height:38px;">N</td>
                              <td style="padding-left:11px;color:#101828;font-size:16px;font-weight:700;line-height:20px;">
                                Nerva Banking
                                <span style="display:block;color:#667085;font-size:10px;font-weight:700;letter-spacing:1.5px;line-height:14px;">BANCA DIGITAL</span>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td class="email-card" style="border:1px solid #d9e2ef;border-radius:18px;background:#ffffff;padding:40px 42px;">
                          <p style="margin:0 0 14px;color:#0878bd;font-size:12px;font-weight:700;letter-spacing:1.4px;line-height:18px;">__EYEBROW__</p>
                          <h1 style="margin:0;color:#101828;font-size:28px;font-weight:700;letter-spacing:-0.5px;line-height:34px;">__TITLE__</h1>
                          __CONTENT__
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:18px 4px 0;color:#667085;font-size:12px;line-height:18px;">
                          <strong style="display:block;margin-bottom:3px;color:#344054;">Proyecto académico</strong>
                          Nerva Banking no es una entidad financiera ni abre cuentas reales. Usá únicamente datos y documentos de prueba.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    private static final Map<NotificationTemplateCode, TemplateDefinition> TEMPLATES = Map.of(
            NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
            new TemplateDefinition(
                    "Continuá tu solicitud en Nerva Banking",
                    "Confirmá tu correo para continuar con la solicitud.",
                    "CONTINUÁ TU SOLICITUD",
                    "Confirmá tu correo",
                    """
                            Recibimos un pedido para comenzar una solicitud con este correo.

                            Continuá desde este enlace:
                            {{magicLink}}

                            Podés usarlo durante {{expiresInMinutes}} minutos.

                            Si no iniciaste una solicitud, ignorá este mensaje.
                            """,
                    """
                            <p style="margin:18px 0 0;color:#475467;font-size:16px;line-height:25px;">Recibimos un pedido para comenzar una solicitud con este correo.</p>
                            __ACTION__
                            <p style="margin:0;color:#667085;font-size:13px;line-height:21px;">Podés usar el enlace durante <strong>{{expiresInMinutes}} minutos</strong>.</p>
                            <p style="margin:18px 0 0;padding-top:18px;border-top:1px solid #e4eaf2;color:#667085;font-size:13px;line-height:21px;">Si no iniciaste una solicitud, ignorá este mensaje.</p>
                            <p style="margin:18px 0 4px;color:#667085;font-size:12px;line-height:18px;">Si el botón no funciona, copiá y pegá esta dirección en el navegador:</p>
                            <p style="margin:0;word-break:break-all;font-size:12px;line-height:18px;"><a href="{{magicLink}}" style="color:#0878bd;text-decoration:underline;">{{magicLink}}</a></p>
                            """,
                    "Continuar solicitud",
                    "magicLink",
                    Set.of("magicLink", "expiresInMinutes"),
                    Set.of("magicLink")
            ),
            NotificationTemplateCode.ONBOARDING_APPROVED_CREDENTIAL_INVITATION,
            new TemplateDefinition(
                    "Creá tu acceso a Nerva Banking",
                    "Tu solicitud fue aprobada. Ya podés crear tu acceso.",
                    "SOLICITUD APROBADA",
                    "Creá tu acceso",
                    """
                            Hola {{firstName}},

                            Tu solicitud fue aprobada. Elegí tu usuario y contraseña desde este enlace:
                            {{credentialSetupLink}}

                            Podés usarlo durante {{expiresInHours}} horas. El enlace es personal y sólo funciona una vez.

                            Si no hiciste esta solicitud, ignorá este mensaje.
                            """,
                    """
                            <p style="margin:18px 0 0;color:#475467;font-size:16px;line-height:25px;">Hola {{firstName}},</p>
                            <p style="margin:12px 0 0;color:#475467;font-size:16px;line-height:25px;">Tu solicitud fue aprobada. Elegí tu usuario y contraseña para terminar.</p>
                            __ACTION__
                            <p style="margin:0;color:#667085;font-size:13px;line-height:21px;">Podés usar el enlace durante <strong>{{expiresInHours}} horas</strong>. Es personal y sólo funciona una vez.</p>
                            <p style="margin:18px 0 0;padding-top:18px;border-top:1px solid #e4eaf2;color:#667085;font-size:13px;line-height:21px;">Si no hiciste esta solicitud, ignorá este mensaje.</p>
                            <p style="margin:18px 0 4px;color:#667085;font-size:12px;line-height:18px;">Si el botón no funciona, copiá y pegá esta dirección en el navegador:</p>
                            <p style="margin:0;word-break:break-all;font-size:12px;line-height:18px;"><a href="{{credentialSetupLink}}" style="color:#0878bd;text-decoration:underline;">{{credentialSetupLink}}</a></p>
                            """,
                    "Crear mi acceso",
                    "credentialSetupLink",
                    Set.of("firstName", "credentialSetupLink", "expiresInHours"),
                    Set.of("credentialSetupLink")
            ),
            NotificationTemplateCode.ONBOARDING_REJECTED,
            new TemplateDefinition(
                    "Sobre tu solicitud en Nerva Banking",
                    "Hay una novedad sobre tu solicitud.",
                    "TU SOLICITUD",
                    "No podemos avanzar con tu solicitud",
                    """
                            Hola {{firstName}},

                            Revisamos tu solicitud y, en esta oportunidad, no podemos avanzar con la apertura.

                            No tenés que hacer nada más.
                            """,
                    """
                            <p style="margin:18px 0 0;color:#475467;font-size:16px;line-height:25px;">Hola {{firstName}},</p>
                            <p style="margin:12px 0 0;color:#475467;font-size:16px;line-height:25px;">Revisamos tu solicitud y, en esta oportunidad, no podemos avanzar con la apertura.</p>
                            <p style="margin:18px 0 0;padding-top:18px;border-top:1px solid #e4eaf2;color:#667085;font-size:13px;line-height:21px;">No tenés que hacer nada más.</p>
                            """,
                    null,
                    null,
                    Set.of("firstName"),
                    Set.of()
            ),
            NotificationTemplateCode.ONBOARDING_COMPLETED,
            new TemplateDefinition(
                    "Tu acceso a Nerva Banking está listo",
                    "Ya terminamos de preparar tu acceso.",
                    "ACCESO LISTO",
                    "Ya podés ingresar",
                    """
                            Hola {{firstName}},

                            Terminamos de preparar tu acceso a Nerva Banking.

                            Ingresá con el usuario y la contraseña que elegiste.
                            """,
                    """
                            <p style="margin:18px 0 0;color:#475467;font-size:16px;line-height:25px;">Hola {{firstName}},</p>
                            <p style="margin:12px 0 0;color:#475467;font-size:16px;line-height:25px;">Terminamos de preparar tu acceso a Nerva Banking.</p>
                            <p style="margin:18px 0 0;padding-top:18px;border-top:1px solid #e4eaf2;color:#667085;font-size:13px;line-height:21px;">Ingresá con el usuario y la contraseña que elegiste.</p>
                            """,
                    null,
                    null,
                    Set.of("firstName"),
                    Set.of()
            )
    );

    private final EmailActionLinkPolicy actionLinkPolicy;

    public InMemoryTemplateRendererAdapter(EmailActionLinkPolicy actionLinkPolicy) {
        this.actionLinkPolicy = actionLinkPolicy;
    }

    @Override
    public RenderedNotification render(NotificationTemplateCode templateCode, Map<String, String> variables) {
        TemplateDefinition template = TEMPLATES.get(templateCode);
        Map<String, String> safeVariables = variables == null ? Map.of() : variables;
        validateVariables(templateCode, template, safeVariables);

        String textBody = "Nerva Banking\n\n"
                + template.title()
                + "\n\n"
                + replaceVariables(template.textBody().strip(), safeVariables, false)
                + "\n\n"
                + ACADEMIC_NOTICE_TEXT;
        String htmlBody = replaceVariables(htmlDocument(template), safeVariables, true);

        return new RenderedNotification(template.subject(), textBody, htmlBody);
    }

    private void validateVariables(
            NotificationTemplateCode templateCode,
            TemplateDefinition template,
            Map<String, String> variables
    ) {
        for (String requiredVariable : template.requiredVariables()) {
            if (!variables.containsKey(requiredVariable)
                    || variables.get(requiredVariable) == null
                    || variables.get(requiredVariable).isBlank()) {
                throw new MissingTemplateVariableException(templateCode, requiredVariable);
            }
        }
        for (String suppliedVariable : variables.keySet()) {
            if (!template.requiredVariables().contains(suppliedVariable)) {
                // Map keys are caller-controlled too. Do not reflect an unexpected key in
                // errors because it could itself contain a token or personal information.
                throw new InvalidTemplateVariableException(templateCode, "variables");
            }
        }
        validateSemanticVariables(templateCode, variables);
        for (String linkVariable : template.linkVariables()) {
            if (!actionLinkPolicy.allows(templateCode, linkVariable, variables.get(linkVariable))) {
                throw new InvalidTemplateVariableException(templateCode, linkVariable);
            }
        }
    }

    private void validateSemanticVariables(
            NotificationTemplateCode templateCode,
            Map<String, String> variables
    ) {
        switch (templateCode) {
            case ONBOARDING_EMAIL_MAGIC_LINK -> validatePositiveInteger(
                    templateCode,
                    "expiresInMinutes",
                    variables.get("expiresInMinutes"),
                    MAX_EXPIRATION_MINUTES
            );
            case ONBOARDING_APPROVED_CREDENTIAL_INVITATION -> {
                validateHumanFirstName(templateCode, variables.get("firstName"));
                validatePositiveInteger(
                        templateCode,
                        "expiresInHours",
                        variables.get("expiresInHours"),
                        MAX_EXPIRATION_HOURS
                );
            }
            case ONBOARDING_REJECTED, ONBOARDING_COMPLETED ->
                    validateHumanFirstName(templateCode, variables.get("firstName"));
        }
    }

    private void validateHumanFirstName(NotificationTemplateCode templateCode, String value) {
        boolean valid = isTrimmed(value)
                && value.codePointCount(0, value.length()) <= MAX_FIRST_NAME_CODE_POINTS
                && HUMAN_FIRST_NAME_PATTERN.matcher(value).matches()
                && value.codePoints().anyMatch(Character::isLetter);
        if (!valid) {
            throw new InvalidTemplateVariableException(templateCode, "firstName");
        }
    }

    private boolean isTrimmed(String value) {
        int first = value.codePointAt(0);
        int last = value.codePointBefore(value.length());
        return Character.getType(first) != Character.SPACE_SEPARATOR
                && Character.getType(last) != Character.SPACE_SEPARATOR;
    }

    private void validatePositiveInteger(
            NotificationTemplateCode templateCode,
            String variableName,
            String value,
            int maximum
    ) {
        if (!POSITIVE_INTEGER_PATTERN.matcher(value).matches()) {
            throw new InvalidTemplateVariableException(templateCode, variableName);
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > maximum) {
                throw new InvalidTemplateVariableException(templateCode, variableName);
            }
        } catch (NumberFormatException exception) {
            throw new InvalidTemplateVariableException(templateCode, variableName);
        }
    }

    private String htmlDocument(TemplateDefinition template) {
        String action = "";
        if (template.actionUrlVariable() != null) {
            action = ACTION_HTML
                    .replace("{{actionUrl}}", "{{" + template.actionUrlVariable() + "}}")
                    .replace("{{actionLabel}}", template.actionLabel());
        }
        return HTML_LAYOUT
                .replace("__SUBJECT__", template.subject())
                .replace("__PREHEADER__", template.preheader())
                .replace("__EYEBROW__", template.eyebrow())
                .replace("__TITLE__", template.title())
                .replace("__CONTENT__", template.htmlContent().strip())
                .replace("__ACTION__", action.strip());
    }

    private String replaceVariables(String source, Map<String, String> variables, boolean escapeHtml) {
        Matcher matcher = VARIABLE_PATTERN.matcher(source);
        StringBuilder rendered = new StringBuilder(source.length());
        while (matcher.find()) {
            String value = variables.getOrDefault(matcher.group(1), "");
            if (escapeHtml) {
                value = HtmlUtils.htmlEscape(value, "UTF-8");
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private record TemplateDefinition(
            String subject,
            String preheader,
            String eyebrow,
            String title,
            String textBody,
            String htmlContent,
            String actionLabel,
            String actionUrlVariable,
            Set<String> requiredVariables,
            Set<String> linkVariables
    ) {
    }
}
