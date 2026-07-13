<#import "template.ftl" as layout>
<#assign bankingFrontendUrl = properties.bankingFrontendUrl!"http://localhost:4200">

<@layout.registrationLayout displayMessage=false; section>
  <#if section = "header">
    <#if messageHeader??>
      ${kcSanitize(msg("${messageHeader}"))?no_esc}
    <#else>
      ${kcSanitize(message.summary)?no_esc}
    </#if>
  <#elseif section = "form">
    <div id="kc-info-message" class="banking-status banking-status--success" role="status">
      <span class="banking-status-icon banking-icon banking-icon--check" aria-hidden="true"></span>
      <p>
        ${kcSanitize(message.summary)?no_esc}
        <#if requiredActions??>
          <#list requiredActions>: <strong><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></strong></#list>
        </#if>
      </p>
      <#if !(skipLink??)>
        <#if pageRedirectUri?has_content>
          <div class="banking-actions">
            <a class="banking-action-link" href="${pageRedirectUri}">${msg("backToApplication")}</a>
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
</@layout.registrationLayout>
