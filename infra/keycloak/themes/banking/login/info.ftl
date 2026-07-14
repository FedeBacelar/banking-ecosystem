<#import "template.ftl" as layout>
<#assign bankingFrontendUrl = properties.bankingFrontendUrl!"http://localhost:4200">
<#assign isCredentialSetup = requiredActions??
  && requiredActions?seq_contains("UPDATE_PROFILE")
  && requiredActions?seq_contains("UPDATE_PASSWORD")>
<#assign credentialCompletionTarget = "">
<#if pageRedirectUri?has_content>
  <#assign credentialCompletionTarget = pageRedirectUri>
<#elseif actionUri?has_content>
  <#assign credentialCompletionTarget = actionUri>
</#if>
<#assign isCredentialSetupComplete = !isCredentialSetup
  && credentialCompletionTarget?contains("/web/auth/login/onboarding-completion")>
<#assign pageSubtitleKey = "bankingLoginSubtitle">
<#assign brandDescriptionKey = "bankingBrandDescription">
<#if isCredentialSetup>
  <#assign pageSubtitleKey = "bankingCredentialSetupIntroSubtitle">
  <#assign brandDescriptionKey = "bankingCredentialSetupIntroBrandDescription">
<#elseif isCredentialSetupComplete>
  <#assign pageSubtitleKey = "bankingCredentialSetupCompleteSubtitle">
  <#assign brandDescriptionKey = "bankingCredentialSetupCompleteBrandDescription">
</#if>

<@layout.registrationLayout
  displayMessage=false
  pageSubtitleKey=pageSubtitleKey
  brandDescriptionKey=brandDescriptionKey
  backLabelKey="bankingExitCredentialSetup";
  section
>
  <#if section = "header">
    <#if isCredentialSetup>
      ${msg("bankingCredentialSetupIntroTitle")}
    <#elseif isCredentialSetupComplete>
      ${msg("bankingCredentialSetupCompleteTitle")}
    <#elseif messageHeader??>
      ${kcSanitize(msg("${messageHeader}"))?no_esc}
    <#else>
      ${kcSanitize(message.summary)?no_esc}
    </#if>
  <#elseif section = "form">
    <#if isCredentialSetup>
      <div id="kc-info-message" class="banking-status">
        <span class="banking-status-icon" aria-hidden="true">
          <span class="banking-icon banking-icon--user-round"></span>
        </span>
        <p>${msg("bankingCredentialSetupIntroDescription")}</p>
        <#if !(skipLink??)>
          <#if actionUri?has_content>
            <div class="banking-actions">
              <a class="banking-action-button" href="${actionUri}">${msg("bankingCredentialSetupIntroAction")}</a>
            </div>
          <#elseif pageRedirectUri?has_content>
            <div class="banking-actions">
              <a class="banking-action-button" href="${pageRedirectUri}">${msg("bankingCredentialSetupIntroAction")}</a>
            </div>
          </#if>
        </#if>
      </div>
    <#elseif isCredentialSetupComplete>
      <div id="kc-info-message" class="banking-status banking-status--success" role="status">
        <span class="banking-status-icon" aria-hidden="true">
          <span class="banking-icon banking-icon--check"></span>
        </span>
        <p>${msg("bankingCredentialSetupCompleteDescription")}</p>
        <div class="banking-actions">
          <a class="banking-action-button" href="${credentialCompletionTarget}">${msg("backToApplication")}</a>
        </div>
      </div>
    <#else>
      <div id="kc-info-message" class="banking-status banking-status--success" role="status">
        <span class="banking-status-icon" aria-hidden="true">
          <span class="banking-icon banking-icon--check"></span>
        </span>
        <p>
          ${kcSanitize(message.summary)?no_esc}
          <#if requiredActions??>
            <#list requiredActions>: <strong><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></strong></#list>
          </#if>
        </p>
        <#if !(skipLink??)>
          <#if pageRedirectUri?has_content>
            <div class="banking-actions">
              <a class="banking-action-button" href="${pageRedirectUri}">${msg("backToApplication")}</a>
            </div>
          <#elseif actionUri?has_content>
            <div class="banking-actions">
              <a class="banking-action-button" href="${actionUri}">${msg("proceedWithAction")}</a>
            </div>
          <#else>
            <div class="banking-actions">
              <a class="banking-action-link" href="${bankingFrontendUrl}">${msg("bankingBackHome")}</a>
            </div>
          </#if>
        </#if>
      </div>
    </#if>
  </#if>
</@layout.registrationLayout>
