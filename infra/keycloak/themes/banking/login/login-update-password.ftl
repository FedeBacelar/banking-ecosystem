<#import "template.ftl" as layout>
<#assign hasNewPasswordError = messagesPerField.existsError('password')>
<#assign hasConfirmationError = messagesPerField.existsError('password-confirm')>
<#assign minimumPasswordLength = passwordPolicies.length!15>

<@layout.registrationLayout
  displayMessage=(messagesPerField.exists('global') && message?has_content && message.type == 'error')
  pageSubtitleKey="bankingCreatePasswordSubtitle"
  brandDescriptionKey="bankingCreatePasswordBrandDescription"
  backLabelKey="bankingExitCredentialSetup";
  section
>
  <#if section = "header">
    ${msg("bankingCreatePasswordTitle")}
  <#elseif section = "form">
    <form
      id="kc-passwd-update-form"
      class="banking-form"
      action="${url.loginAction}"
      method="post"
      novalidate="novalidate"
      data-banking-loading-form
      data-loading-announcement="${msg('bankingCreatePasswordLoadingAnnouncement')}"
    >
      <div class="banking-field">
        <label class="banking-label" for="password-new">${msg("passwordNew")}</label>
        <div class="banking-password-control<#if hasNewPasswordError> banking-password-control--invalid</#if>">
          <input
            id="password-new"
            class="banking-input banking-input--password"
            name="password-new"
            type="password"
            required
            minlength="${minimumPasswordLength}"
            autocomplete="new-password"
            autofocus
            aria-required="true"
            aria-describedby="password-new-hint<#if hasNewPasswordError> password-new-error</#if>"
            <#if hasNewPasswordError>aria-invalid="true"</#if>
          />
          <button
            class="banking-password-toggle"
            type="button"
            aria-label="${msg('showPassword')}"
            aria-controls="password-new"
            data-banking-password-toggle
            data-label-show="${msg('showPassword')}"
            data-label-hide="${msg('hidePassword')}"
          >
            <span class="banking-icon banking-icon--eye" aria-hidden="true"></span>
          </button>
        </div>
        <p id="password-new-hint" class="banking-field-hint">${msg("bankingCreatePasswordHint", minimumPasswordLength)}</p>
        <#if hasNewPasswordError>
          <p id="password-new-error" class="banking-field-error" role="alert">${kcSanitize(messagesPerField.get('password'))?no_esc}</p>
        </#if>
      </div>

      <div class="banking-field">
        <label class="banking-label" for="password-confirm">${msg("passwordConfirm")}</label>
        <div class="banking-password-control<#if hasConfirmationError> banking-password-control--invalid</#if>">
          <input
            id="password-confirm"
            class="banking-input banking-input--password"
            name="password-confirm"
            type="password"
            required
            minlength="${minimumPasswordLength}"
            autocomplete="new-password"
            aria-required="true"
            <#if hasConfirmationError>aria-invalid="true" aria-describedby="password-confirm-error"</#if>
          />
          <button
            class="banking-password-toggle"
            type="button"
            aria-label="${msg('showPassword')}"
            aria-controls="password-confirm"
            data-banking-password-toggle
            data-label-show="${msg('showPassword')}"
            data-label-hide="${msg('hidePassword')}"
          >
            <span class="banking-icon banking-icon--eye" aria-hidden="true"></span>
          </button>
        </div>
        <#if hasConfirmationError>
          <p id="password-confirm-error" class="banking-field-error" role="alert">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</p>
        </#if>
      </div>

      <div class="banking-form-actions">
        <button
          class="banking-submit"
          name="login"
          type="submit"
          data-banking-submit
          data-loading-label="${msg('bankingCreatePasswordLoading')}"
        >
          <span class="banking-icon banking-icon--lock banking-submit__idle-icon" aria-hidden="true"></span>
          <span class="banking-submit__spinner" aria-hidden="true"></span>
          <span class="banking-submit__label">${msg("bankingCreatePasswordAction")}</span>
        </button>
        <#if isAppInitiatedAction??>
          <button class="banking-secondary-button" type="submit" name="cancel-aia" value="true" formnovalidate data-banking-cancel>${msg("doCancel")}</button>
        </#if>
      </div>
      <span class="banking-sr-only" role="status" aria-live="polite" data-banking-progress></span>
    </form>
  </#if>
</@layout.registrationLayout>
