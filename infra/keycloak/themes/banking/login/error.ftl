<#import "template.ftl" as layout>
<#assign bankingFrontendUrl = properties.bankingFrontendUrl!"http://localhost:4200">

<@layout.registrationLayout displayMessage=false; section>
  <#if section = "header">
    ${kcSanitize(msg("errorTitle"))?no_esc}
  <#elseif section = "form">
    <div id="kc-error-message" class="banking-status banking-status--error" role="alert">
      <span class="banking-status-icon banking-icon banking-icon--circle-alert" aria-hidden="true"></span>
      <p>${msg("bankingUnexpectedError")}</p>
      <#if !(skipLink??)>
        <div class="banking-actions">
          <a id="backToApplication" class="banking-action-link" href="${bankingFrontendUrl}">${msg("bankingBackHome")}</a>
        </div>
      </#if>
    </div>
  </#if>
</@layout.registrationLayout>
