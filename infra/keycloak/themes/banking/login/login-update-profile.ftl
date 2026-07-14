<#import "template.ftl" as layout>
<#assign hasUsernameError = messagesPerField.existsError('username')>
<#assign displayedUsername = (user.username!'')>
<#if profile?? && profile.attributes??>
  <#list profile.attributes as attribute>
    <#if attribute.name == "username">
      <#assign displayedUsername = (attribute.value!displayedUsername)>
    </#if>
  </#list>
</#if>
<#if displayedUsername?starts_with("pending-")>
  <#assign displayedUsername = "">
</#if>

<@layout.registrationLayout
  displayMessage=(messagesPerField.exists('global') && message?has_content && message.type == 'error')
  pageSubtitleKey="bankingChooseUsernameSubtitle"
  brandDescriptionKey="bankingChooseUsernameBrandDescription"
  backLabelKey="bankingExitCredentialSetup";
  section
>
  <#if section = "header">
    ${msg("bankingChooseUsernameTitle")}
  <#elseif section = "form">
    <form
      id="kc-update-profile-form"
      class="banking-form"
      action="${url.loginAction}"
      method="post"
      novalidate="novalidate"
      data-banking-loading-form
      data-loading-announcement="${msg('bankingChooseUsernameLoadingAnnouncement')}"
    >
      <div class="banking-field">
        <label class="banking-label" for="username">${msg("username")}</label>
        <input
          id="username"
          class="banking-input"
          name="username"
          value="${displayedUsername}"
          type="text"
          required
          minlength="4"
          maxlength="64"
          autocomplete="username"
          autocapitalize="none"
          spellcheck="false"
          autofocus
          aria-required="true"
          aria-describedby="username-hint<#if hasUsernameError> username-error</#if>"
          <#if hasUsernameError>aria-invalid="true"</#if>
        />
        <p id="username-hint" class="banking-field-hint">${msg("bankingChooseUsernameHint")}</p>
        <#if hasUsernameError>
          <p id="username-error" class="banking-field-error" role="alert">${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}</p>
        </#if>
      </div>

      <div class="banking-form-actions">
        <button
          class="banking-submit"
          type="submit"
          data-banking-submit
          data-loading-label="${msg('bankingChooseUsernameLoading')}"
        >
          <span class="banking-icon banking-icon--user-round banking-submit__idle-icon" aria-hidden="true"></span>
          <span class="banking-submit__spinner" aria-hidden="true"></span>
          <span class="banking-submit__label">${msg("bankingChooseUsernameAction")}</span>
        </button>
        <#if isAppInitiatedAction??>
          <button class="banking-secondary-button" type="submit" name="cancel-aia" value="true" formnovalidate data-banking-cancel>${msg("doCancel")}</button>
        </#if>
      </div>
      <span class="banking-sr-only" role="status" aria-live="polite" data-banking-progress></span>
    </form>
  </#if>
</@layout.registrationLayout>
