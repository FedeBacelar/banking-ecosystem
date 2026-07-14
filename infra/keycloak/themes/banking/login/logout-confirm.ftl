<#import "template.ftl" as layout>
<#assign bankingFrontendUrl = properties.bankingFrontendUrl!"http://localhost:4200">

<@layout.registrationLayout; section>
  <#if section = "header">
    ${msg("logoutConfirmTitle")}
  <#elseif section = "form">
    <div id="kc-logout-confirm" class="banking-status">
      <span class="banking-status-icon" aria-hidden="true">
        <span class="banking-icon banking-icon--lock"></span>
      </span>
      <p>${msg("logoutConfirmHeader")}</p>
      <form class="banking-actions" action="${url.logoutConfirmAction}" onsubmit="confirmLogout.disabled = true; return true;" method="post">
        <input type="hidden" name="session_code" value="${logoutConfirm.code}">
        <button class="banking-action-button" name="confirmLogout" id="kc-logout" type="submit">${msg("doLogout")}</button>
      </form>
      <#if !(logoutConfirm.skipLink)>
        <a class="banking-action-link" href="${bankingFrontendUrl}">${msg("bankingBackHome")}</a>
      </#if>
    </div>
  </#if>
</@layout.registrationLayout>
