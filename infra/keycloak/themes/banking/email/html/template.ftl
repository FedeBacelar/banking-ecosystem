<#ftl output_format="HTML">
<#macro emailLayout>
<!doctype html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="light only">
  <title>Nerva Banking</title>
</head>
<body style="margin:0;padding:0;background:#f4f7fb;color:#111827;font-family:Arial,'Helvetica Neue',sans-serif;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="width:100%;background:#f4f7fb;">
    <tr>
      <td align="center" style="padding:32px 16px;">
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
            <td style="border:1px solid #d9e2ef;border-radius:18px;background:#ffffff;padding:40px 42px;box-shadow:0 12px 30px rgba(15,35,65,0.08);">
              <#nested>
            </td>
          </tr>
          <tr>
            <td style="padding:18px 4px 0;color:#667085;font-size:12px;line-height:18px;">
              <strong style="display:block;margin-bottom:3px;color:#344054;">${msg("bankingEmailAcademicTitle")}</strong>
              ${msg("bankingEmailAcademicDescription")}
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
</#macro>
