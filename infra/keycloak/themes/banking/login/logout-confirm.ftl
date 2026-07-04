<#import "template.ftl" as layout>

<@layout.registrationLayout; section>
  <#if section = "header">
    ${msg("logoutConfirmTitle")}
  <#elseif section = "form">
    <div id="kc-logout-confirm" class="banking-status">
      <span class="banking-status-icon banking-icon banking-icon--lock" aria-hidden="true"></span>
      <p>${msg("logoutConfirmHeader")}</p>
      <form class="banking-actions" action="${url.logoutConfirmAction}" onsubmit="confirmLogout.disabled = true; return true;" method="post">
        <input type="hidden" name="session_code" value="${logoutConfirm.code}">
        <button class="banking-action-button" name="confirmLogout" id="kc-logout" type="submit">${msg("doLogout")}</button>
      </form>
      <#if !(logoutConfirm.skipLink) && (client.baseUrl)?has_content>
        <a class="banking-action-link" href="${client.baseUrl}">${msg("backToApplication")}</a>
      </#if>
    </div>
  </#if>
</@layout.registrationLayout>
