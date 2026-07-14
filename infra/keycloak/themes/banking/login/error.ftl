<#import "template.ftl" as layout>
<#assign bankingFrontendUrl = properties.bankingFrontendUrl!"http://localhost:4200">
<#assign isExpiredCredentialAction = message?has_content && (
  message.summary == msg("expiredActionMessage") ||
  message.summary == msg("expiredActionTokenNoSessionMessage") ||
  message.summary == msg("expiredActionTokenSessionExistsMessage") ||
  message.summary == msg("invalidTokenRequiredActions")
)>

<@layout.registrationLayout
  displayMessage=false
  pageSubtitleKey=(isExpiredCredentialAction?then("bankingExpiredActionSubtitle", "bankingErrorSubtitle"))
  brandDescriptionKey=(isExpiredCredentialAction?then("bankingExpiredActionBrandDescription", "bankingErrorBrandDescription"));
  section
>
  <#if section = "header">
    <#if isExpiredCredentialAction>
      ${msg("bankingExpiredActionTitle")}
    <#else>
      ${kcSanitize(msg("errorTitle"))?no_esc}
    </#if>
  <#elseif section = "form">
    <div id="kc-error-message" class="banking-status banking-status--error" role="alert">
      <span class="banking-status-icon" aria-hidden="true">
        <span class="banking-icon banking-icon--circle-alert"></span>
      </span>
      <p><#if isExpiredCredentialAction>${msg("bankingExpiredActionDescription")}<#else>${msg("bankingUnexpectedError")}</#if></p>
      <#if !(skipLink??)>
        <div class="banking-actions">
          <a id="backToApplication" class="<#if isExpiredCredentialAction>banking-action-button<#else>banking-action-link</#if>" href="${bankingFrontendUrl}"><#if isExpiredCredentialAction>${msg("bankingExpiredActionReturn")}<#else>${msg("bankingBackHome")}</#if></a>
        </div>
      </#if>
    </div>
  </#if>
</@layout.registrationLayout>
