<#ftl output_format="HTML">
<#import "template.ftl" as layout>

<@layout.emailLayout>
  <p style="margin:0 0 14px;color:#0878bd;font-size:12px;font-weight:700;letter-spacing:1.4px;line-height:18px;">${msg("bankingCredentialEmailEyebrow")}</p>
  <h1 style="margin:0;color:#101828;font-size:28px;font-weight:700;letter-spacing:-0.5px;line-height:34px;">${msg("bankingCredentialEmailTitle")}</h1>
  <p style="margin:18px 0 0;color:#475467;font-size:16px;line-height:25px;">${msg("bankingCredentialEmailDescription")}</p>

  <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:28px 0 24px;">
    <tr>
      <td align="center" style="border-radius:9px;background:#173b6c;">
        <a href="${link}" style="display:inline-block;padding:14px 22px;color:#ffffff;font-size:15px;font-weight:700;line-height:20px;text-decoration:none;">${msg("bankingCredentialEmailAction")}</a>
      </td>
    </tr>
  </table>

  <p style="margin:0;color:#667085;font-size:13px;line-height:21px;">${kcSanitize(msg("bankingCredentialEmailExpiration", linkExpirationFormatter(linkExpiration)))?no_esc}</p>
  <p style="margin:18px 0 0;padding-top:18px;border-top:1px solid #e4eaf2;color:#667085;font-size:13px;line-height:21px;">${msg("bankingCredentialEmailIgnore")}</p>
</@layout.emailLayout>
