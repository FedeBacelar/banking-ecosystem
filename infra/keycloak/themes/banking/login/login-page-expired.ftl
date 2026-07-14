<#import "template.ftl" as layout>

<@layout.registrationLayout; section>
  <#if section = "header">
    ${msg("pageExpiredTitle")}
  <#elseif section = "form">
    <div id="kc-page-expired" class="banking-status" role="status">
      <span class="banking-status-icon" aria-hidden="true">
        <span class="banking-icon banking-icon--key-round"></span>
      </span>
      <p>${msg("pageExpiredMsg1")}</p>
      <div class="banking-actions">
        <a id="loginRestartLink" class="banking-action-button" href="${url.loginRestartFlowUrl}">${msg("bankingRestartLogin")}</a>
        <a id="loginContinueLink" class="banking-action-link" href="${url.loginAction}">${msg("bankingContinueLogin")}</a>
      </div>
    </div>
  </#if>
</@layout.registrationLayout>
