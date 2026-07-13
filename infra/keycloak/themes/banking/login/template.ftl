<#import "field.ftl" as field>
<#import "footer.ftl" as loginFooter>
<#assign bankingFrontendUrl = properties.bankingFrontendUrl!"http://localhost:4200">

<#macro username>
  <#assign label>
    <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
  </#assign>
  <@field.group name="username" label=label>
    <div class="${properties.kcInputGroup}">
      <div class="${properties.kcInputGroupItemClass} ${properties.kcFill}">
        <span class="${properties.kcInputClass} ${properties.kcFormReadOnlyClass}">
          <input id="kc-attempted-username" value="${auth.attemptedUsername}" readonly>
        </span>
      </div>
      <div class="${properties.kcInputGroupItemClass}">
        <button id="reset-login" class="${properties.kcFormPasswordVisibilityButtonClass} kc-login-tooltip" type="button"
              aria-label="${msg('restartLoginTooltip')}" onclick="location.href='${url.loginRestartFlowUrl}'">
            <i class="fa-sync-alt fas" aria-hidden="true"></i>
            <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
        </button>
      </div>
    </div>
  </@field.group>
</#macro>

<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${lang}"<#if realm.internationalizationEnabled> dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="color-scheme" content="light">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nerva Banking | ${title!msg("loginAccountTitle")}</title>
    <link rel="icon" href="${url.resourcesPath}/img/banking-logo.svg" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    <script type="module">
        <#outputformat "JavaScript">
        import { startSessionPolling } from ${(url.resourcesPath + "/js/authChecker.js")?c};
        startSessionPolling(${url.ssoLoginInOtherTabsUrl?c});
        </#outputformat>
    </script>
    <#if authenticationSession??>
        <script type="module">
             <#outputformat "JavaScript">
            import { checkAuthSession } from ${(url.resourcesPath + "/js/authChecker.js")?c};
            checkAuthSession(${authenticationSession.authSessionIdHash?c});
            </#outputformat>
        </script>
    </#if>
    <script>
      const isFirefox = true;
    </script>
</head>

<body id="keycloak-bg" class="${properties.kcBodyClass!}" data-page-id="login-${pageId}">
  <main class="banking-shell">
    <aside class="banking-brand-panel" aria-label="${msg("bankingBrandAria")}">
      <div class="banking-grid" aria-hidden="true"></div>
      <div class="banking-brand-panel__top">
        <img class="banking-logo banking-logo--light" src="${url.resourcesPath}/img/banking-logo-light.svg" alt="Nerva Banking">
      </div>
      <section class="banking-brand-panel__content">
        <span class="banking-badge">
          ${msg("bankingAccessLabel")}
        </span>
        <h2>${msg("bankingBrandTitle")}</h2>
        <p>${msg("bankingBrandDescription")}</p>
      </section>
      <p class="banking-brand-panel__footer">Nerva Banking</p>
    </aside>

    <section class="banking-auth-panel">
      <div class="banking-auth-panel__top">
        <a class="banking-back-link" href="${bankingFrontendUrl}">
          <span class="banking-icon banking-icon--arrow-left" aria-hidden="true"></span>
          ${msg("bankingBackHome")}
        </a>
        <span class="banking-mobile-brand">
          Nerva Banking
        </span>
      </div>

      <div class="banking-auth-panel__center">
        <div class="banking-login-container">
          <img class="banking-logo banking-logo--mobile" src="${url.resourcesPath}/img/banking-logo.svg" alt="Nerva Banking">

          <section class="banking-login-card" aria-labelledby="kc-page-title">
            <header class="banking-login-card__header">
              <h1 id="kc-page-title"><#nested "header"></h1>
              <p>${msg("bankingLoginSubtitle")}</p>
            </header>

            <div class="banking-divider" aria-hidden="true"></div>

            <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                <#if displayRequiredFields>
                    <p class="banking-required-note"><span>*</span> ${msg("requiredFields")}</p>
                </#if>
            <#else>
                <div class="${properties.kcFormClass} banking-attempted-username">
                  <#nested "show-username">
                  <@username />
                </div>
            </#if>

            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="${properties.kcAlertClass!} pf-m-${(message.type = 'error')?then('danger', message.type)} banking-alert banking-alert--${message.type}" <#if message.type = 'error'>role="alert" aria-live="assertive"<#else>role="status" aria-live="polite"</#if>>
                    <div class="${properties.kcAlertIconClass!}">
                        <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                        <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                        <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                        <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                    </div>
                    <span class="${properties.kcAlertTitleClass!} kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#nested "form">

            <#if auth?has_content && auth.showTryAnotherWayLink()>
              <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post" novalidate="novalidate">
                  <input type="hidden" name="tryAnotherWay" value="on"/>
                  <a id="try-another-way" href="javascript:document.forms['kc-select-try-another-way-form'].requestSubmit()"
                      class="${properties.kcButtonSecondaryClass} ${properties.kcButtonBlockClass} ${properties.kcMarginTopClass}">
                        ${msg("doTryAnotherWay")}
                  </a>
              </form>
            </#if>

            <div class="banking-social-providers">
              <#nested "socialProviders">
            </div>

            <#if displayInfo>
              <div id="kc-info" class="banking-info">
                <#nested "info">
              </div>
            </#if>

          </section>

          <aside class="banking-academic-note" aria-label="${msg('bankingDisclaimerTitle')}">
            <span class="banking-icon banking-icon--info" aria-hidden="true"></span>
            <span>
              <strong>${msg("bankingDisclaimerTitle")}</strong>
              <span>${msg("bankingDisclaimer")}</span>
            </span>
          </aside>
        </div>
      </div>
    </section>
  </main>
</body>
</html>
</#macro>
