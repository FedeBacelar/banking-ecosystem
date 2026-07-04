<#import "template.ftl" as layout>
<#import "passkeys.ftl" as passkeys>
<#import "social-providers.ftl" as identityProviders>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
  <#if section = "header">
    ${msg("loginAccountTitle")}
  <#elseif section = "form">
    <#if realm.password>
      <form id="kc-form-login" class="banking-form" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate="novalidate">
        <#if !usernameHidden??>
          <#assign usernameLabel>
            <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
          </#assign>
          <div class="banking-field">
            <label class="banking-label" for="username">${usernameLabel}</label>
            <input
              id="username"
              class="banking-input"
              name="username"
              value="${(login.username!'')}"
              type="text"
              autocomplete="${(enableWebAuthnConditionalUI?has_content)?then('username webauthn', 'username')}"
              autofocus
              aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
            />
            <#if messagesPerField.existsError('username','password')>
              <p class="banking-field-error">${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}</p>
            </#if>
          </div>
        </#if>

        <div class="banking-field">
          <div class="banking-label-row">
            <label class="banking-label" for="password">${msg("password")}</label>
          </div>
          <div class="banking-password-control">
            <input
              id="password"
              class="banking-input banking-input--password"
              name="password"
              type="password"
              autocomplete="current-password"
              <#if usernameHidden??>autofocus</#if>
              aria-invalid="<#if messagesPerField.existsError('password')>true</#if>"
            />
            <button
              id="password-show-password"
              class="banking-password-toggle"
              type="button"
              aria-label="${msg('showPassword')}"
              aria-controls="password"
              data-label-show="${msg('showPassword')}"
              data-label-hide="${msg('hidePassword')}"
            >
              <span class="banking-icon banking-icon--eye" aria-hidden="true"></span>
            </button>
          </div>
        </div>

        <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

        <button id="kc-login" class="banking-submit" name="login" type="submit">
          <span class="banking-icon banking-icon--lock" aria-hidden="true"></span>
          ${msg("doLogIn")}
        </button>
      </form>
    </#if>
    <@passkeys.conditionalUIData />
  <#elseif section = "socialProviders">
    <#if realm.password && social.providers?? && social.providers?has_content>
      <@identityProviders.show social=social/>
    </#if>
  <#elseif section = "info">
    <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
      <div id="kc-registration-container">
        <div id="kc-registration">
          <span>${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a></span>
        </div>
      </div>
    </#if>
  </#if>
</@layout.registrationLayout>
