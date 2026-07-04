<#import "template.ftl" as layout>

<@layout.registrationLayout displayMessage=false; section>
  <#if section = "header">
    ${kcSanitize(msg("errorTitle"))?no_esc}
  <#elseif section = "form">
    <div id="kc-error-message" class="banking-status banking-status--error">
      <span class="banking-status-icon banking-icon banking-icon--circle-alert" aria-hidden="true"></span>
      <p>${kcSanitize((message.summary)!msg("bankingUnexpectedError"))?no_esc}</p>
      <#if traceId??>
        <p id="traceId">${msg("traceIdSupportMessage", traceId)}</p>
      </#if>
      <#if !(skipLink??) && client?? && client.baseUrl?has_content>
        <div class="banking-actions">
          <a id="backToApplication" class="banking-action-link" href="${client.baseUrl}">${msg("backToApplication")}</a>
        </div>
      </#if>
    </div>
  </#if>
</@layout.registrationLayout>
