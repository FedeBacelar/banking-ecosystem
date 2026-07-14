import { access, readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryDirectory = path.resolve(scriptDirectory, "..", "..", "..");
const keycloakDirectory = path.join(repositoryDirectory, "infra", "keycloak");
const loginThemeDirectory = path.join(keycloakDirectory, "themes", "banking", "login");
const emailThemeDirectory = path.join(keycloakDirectory, "themes", "banking", "email");
const expectedPasswordPolicy =
  "length(15) and maxLength(64) and notUsername and notEmail and passwordBlacklist(nerva-passwords.txt)";
const expectedCredentialActionPriorities = {
  UPDATE_PROFILE: 30,
  UPDATE_PASSWORD: 40,
};
const expectedRedirectUris = [
  "http://localhost:8085/web/login/oauth2/code/keycloak",
  "http://localhost:8085/web/login/oauth2/code/keycloak-onboarding-completion",
  "http://localhost:8085/web/auth/login/onboarding-completion",
  "${BANKING_FRONTEND_URL}/onboarding/credentials-complete",
];
const expectedLiveRedirectUris = expectedRedirectUris.map((redirectUri) =>
  redirectUri.replace(
    "${BANKING_FRONTEND_URL}",
    (process.env.BANKING_FRONTEND_URL ?? "http://localhost:4200").replace(/\/$/, ""),
  ),
);

const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

const realm = JSON.parse(
  await readFile(
    path.join(keycloakDirectory, "realms", "banking-ecosystem-realm.json"),
    "utf8",
  ),
);
const themeProperties = await readFile(
  path.join(loginThemeDirectory, "theme.properties"),
  "utf8",
);
const template = await readFile(path.join(loginThemeDirectory, "template.ftl"), "utf8");
const login = await readFile(path.join(loginThemeDirectory, "login.ftl"), "utf8");
const info = await readFile(path.join(loginThemeDirectory, "info.ftl"), "utf8");
const errorPage = await readFile(path.join(loginThemeDirectory, "error.ftl"), "utf8");
const expiredPage = await readFile(
  path.join(loginThemeDirectory, "login-page-expired.ftl"),
  "utf8",
);
const logoutConfirm = await readFile(
  path.join(loginThemeDirectory, "logout-confirm.ftl"),
  "utf8",
);
const updateProfile = await readFile(
  path.join(loginThemeDirectory, "login-update-profile.ftl"),
  "utf8",
);
const updatePassword = await readFile(
  path.join(loginThemeDirectory, "login-update-password.ftl"),
  "utf8",
);
const messages = await readFile(
  path.join(loginThemeDirectory, "messages", "messages_es.properties"),
  "utf8",
);
const styles = await readFile(
  path.join(loginThemeDirectory, "resources", "css", "banking-login.css"),
  "utf8",
);
const behavior = await readFile(
  path.join(loginThemeDirectory, "resources", "js", "banking-login.js"),
  "utf8",
);
const emailThemeProperties = await readFile(
  path.join(emailThemeDirectory, "theme.properties"),
  "utf8",
);
const emailLayout = await readFile(
  path.join(emailThemeDirectory, "html", "template.ftl"),
  "utf8",
);
const emailHtml = await readFile(
  path.join(emailThemeDirectory, "html", "executeActions.ftl"),
  "utf8",
);
const emailText = await readFile(
  path.join(emailThemeDirectory, "text", "executeActions.ftl"),
  "utf8",
);
const emailMessages = await readFile(
  path.join(emailThemeDirectory, "messages", "messages_es.properties"),
  "utf8",
);
const compose = await readFile(path.join(keycloakDirectory, "docker-compose.yml"), "utf8");
const userProfile = JSON.parse(
  await readFile(path.join(keycloakDirectory, "realms", "banking-user-profile.json"), "utf8"),
);
const passwordBlacklist = await readFile(
  path.join(keycloakDirectory, "password-blacklists", "nerva-passwords.txt"),
  "utf8",
);

assert(realm.loginTheme === "banking", "The banking login theme is not selected.");
assert(realm.emailTheme === "banking", "The banking email theme is not selected.");
assert(
  realm.passwordPolicy === expectedPasswordPolicy,
  "The customer password policy does not match the approved contract.",
);
for (const [alias, priority] of Object.entries(expectedCredentialActionPriorities)) {
  const requiredAction = realm.requiredActions?.find((candidate) => candidate.alias === alias);
  assert(
    requiredAction?.enabled === true && requiredAction.priority === priority,
    `The ${alias} required action is not enabled at the approved priority.`,
  );
}
assert(
  JSON.stringify(
    realm.clients.find((client) => client.clientId === "home-banking-bff")?.redirectUris,
  ) === JSON.stringify(expectedRedirectUris),
  "The browser client redirect URI contract is incomplete.",
);
assert(realm.defaultLocale === "es", "Spanish is not the default realm locale.");
assert(
  JSON.stringify(realm.supportedLocales) === JSON.stringify(["es"]),
  "The customer realm must support Spanish only.",
);
assert(/^locales=es$/m.test(themeProperties), "The login theme must expose Spanish only.");
assert(/^locales=es$/m.test(emailThemeProperties), "The email theme must expose Spanish only.");
assert(/^parent=base$/m.test(emailThemeProperties), "The email theme must inherit the base contracts.");
assert(
  themeProperties.includes("${env.BANKING_FRONTEND_URL\\:http://localhost:4200}"),
  "The frontend return URL is not environment-aware.",
);

const customerFacingSources = `${template}\n${login}\n${info}\n${messages}`;
assert(
  !/(proyecto demo|portfolio|protegido por Keycloak|\/web\/session)/i.test(
    customerFacingSources,
  ),
  "Demo or technical copy remains in the customer-facing theme.",
);
assert(
  !/(experiencia clara|entorno protegido|espacio personal para operar)/i.test(
    customerFacingSources,
  ),
  "Customer-facing theme copy still contains generic marketing filler.",
);
assert(
  customerFacingSources.includes("Nerva Banking no es una entidad financiera"),
  "The academic disclaimer is missing from the customer-facing theme.",
);
assert(
  login.includes('aria-describedby="login-error"'),
  "Login errors are not associated with their inputs.",
);
assert(
  login.includes("data-loading-announcement"),
  "The login form has no accessible loading announcement.",
);
assert(
  behavior.includes('setAttribute("aria-busy", "true")'),
  "The progressive loading behavior is missing.",
);
assert(
  updateProfile.includes('name="username"') &&
    updateProfile.includes('aria-required="true"') &&
    updateProfile.includes("profile.attributes") &&
    updateProfile.includes('?starts_with("pending-")') &&
    !/(name="email"|name="firstName"|name="lastName")/.test(updateProfile),
  "The username action must hide the provisional identifier and not expose locked applicant fields.",
);
assert(
  updatePassword.includes('name="password-new"') &&
    updatePassword.includes('name="password-confirm"') &&
    updatePassword.match(/aria-required="true"/g)?.length === 2 &&
    updatePassword.includes("passwordPolicies.length") &&
    updatePassword.includes("passwordPolicies.maxLength") &&
    /^bankingCreatePasswordHint=Usá entre \{0\} y \{1\} caracteres\. Podés incluir espacios\.$/m.test(messages) &&
    /^invalidPasswordMaxLengthMessage=Usá hasta \{0\} caracteres\.$/m.test(messages),
  "The password action is not bound to the native Keycloak contract.",
);
assert(
  updateProfile.includes("data-banking-loading-form") &&
    updatePassword.includes("data-banking-loading-form") &&
    behavior.includes('[data-banking-loading-form]'),
  "Required-action forms have no accessible loading behavior.",
);
assert(
  updateProfile.includes("message.type == 'error'") &&
    updatePassword.includes("message.type == 'error'"),
  "Required-action screens still render non-actionable global prompts.",
);
assert(
  behavior.includes('[data-banking-password-toggle]'),
  "Required-action password visibility is not progressively enhanced.",
);
assert(
  info.includes('<a class="banking-action-button" href="${pageRedirectUri}">') &&
    /^backToApplication=Continuar$/m.test(messages),
  "Credential completion does not expose a clear primary continuation action.",
);
assert(
  info.includes('requiredActions?seq_contains("UPDATE_PROFILE")') &&
    info.includes('requiredActions?seq_contains("UPDATE_PASSWORD")') &&
    info.includes('href="${actionUri}"') &&
    /^bankingCredentialSetupIntroTitle=Creá tu acceso$/m.test(messages) &&
    /^bankingCredentialSetupIntroAction=Empezar$/m.test(messages),
  "The credential setup introduction still depends on generic Keycloak copy.",
);
assert(
  info.includes('credentialCompletionTarget?contains("/web/auth/login/onboarding-completion")') &&
    /^bankingCredentialSetupCompleteTitle=Acceso creado$/m.test(messages) &&
    /^bankingCredentialSetupCompleteDescription=Continuá para terminar de preparar tu cuenta\.$/m.test(messages),
  "The credential completion still depends on generic Keycloak copy.",
);
assert(
  errorPage.includes('message.summary == msg("expiredActionMessage")') &&
    errorPage.includes('bankingExpiredActionDescription'),
  "Used or expired credential links do not expose the approved recovery copy.",
);
const statusTemplates = `${info}\n${errorPage}\n${expiredPage}\n${logoutConfirm}`;
assert(
  !statusTemplates.includes("banking-status-icon banking-icon") &&
    statusTemplates.match(/class="banking-status-icon"/g)?.length === 6 &&
    styles.includes(".banking-status-icon > .banking-icon"),
  "Status icon masks must be separate from their colored background containers.",
);
assert(
  !/(upperCase|lowerCase|digits|specialChars|forceExpiredPasswordChange)/.test(
    realm.passwordPolicy,
  ),
  "Artificial composition or periodic expiration remains in the password policy.",
);
assert(
  !passwordBlacklist.includes("\r") &&
    passwordBlacklist.includes("nerva banking 2026") &&
    passwordBlacklist.includes("correcthorsebatterystaple"),
  "The repository-owned UTF-8 password blocklist is missing its deterministic checks.",
);
assert(
  compose.includes("./password-blacklists:/opt/keycloak/data/password-blacklists:ro") &&
    compose.includes("-s 'emailTheme=banking'") &&
    compose.includes(`-s 'passwordPolicy=${expectedPasswordPolicy}'`) &&
    compose.includes("authentication/required-actions/UPDATE_PROFILE") &&
    compose.includes("authentication/required-actions/UPDATE_PASSWORD") &&
    compose.includes('\\"fromDisplayName\\":\\"Nerva Banking\\"'),
  "The realm initializer does not reconcile email, credential actions, password or sender configuration.",
);

const emailCustomerFacingSources = `${emailLayout}\n${emailHtml}\n${emailText}\n${emailMessages}`;
assert(
  !/(administrador|actualiza tu cuenta|actualizar perfil|keycloak)/i.test(
    emailCustomerFacingSources,
  ),
  "The credential email still contains generic administration or implementation copy.",
);
assert(
  emailMessages.includes("executeActionsSubject=Completá tu acceso de prueba a Nerva Banking") &&
    emailHtml.includes('href="${link}"') &&
    emailText.includes("${link}") &&
    emailHtml.includes("linkExpirationFormatter(linkExpiration)") &&
    emailText.includes("linkExpirationFormatter(linkExpiration)"),
  "The credential email is missing its subject, action link or human expiration.",
);
assert(
  emailLayout.includes('role="presentation"') &&
    emailText.includes('msg("bankingEmailAcademicTitle")') &&
    emailMessages.includes("Nerva Banking no es una entidad financiera"),
  "The accessible email layout, plain-text alternative or academic disclaimer is missing.",
);

const usernameAttribute = userProfile.attributes.find(
  (attribute) => attribute.name === "username",
);
assert(
  usernameAttribute?.permissions?.edit?.includes("user") &&
    usernameAttribute?.validations?.length?.min === 4 &&
    usernameAttribute?.validations?.length?.max === 64,
  "The username required action is not backed by the restricted user profile.",
);
assert(styles.includes('@font-face'), "The self-hosted Geist font is not declared.");
assert(
  styles.includes("@media (min-width: 1120px)"),
  "The safe desktop split breakpoint is missing.",
);
assert(
  styles.includes("@media (prefers-reduced-motion: reduce)"),
  "Reduced-motion behavior is missing.",
);

await access(
  path.join(
    loginThemeDirectory,
    "resources",
    "fonts",
    "geist-latin-wght-normal.woff2",
  ),
);

if (process.argv.includes("--live")) {
  const origin = process.env.KEYCLOAK_URL ?? "http://localhost:8090";
  const authorizationUrl = new URL(
    "/realms/banking-ecosystem/protocol/openid-connect/auth",
    origin,
  );
  authorizationUrl.search = new URLSearchParams({
    client_id: "home-banking-bff",
    redirect_uri: "http://localhost:8085/web/login/oauth2/code/keycloak",
    response_type: "code",
    scope: "openid",
    state: "theme-verification",
    nonce: "theme-verification",
  });

  const response = await fetch(authorizationUrl, { signal: AbortSignal.timeout(10_000) });
  assert(response.ok, `Keycloak returned HTTP ${response.status}.`);
  const html = await response.text();
  assert(html.includes("Volver al inicio"), "The rendered page is not in Spanish.");
  assert(
    html.includes("Nerva Banking no es una entidad financiera"),
    "The rendered page does not show the academic disclaimer.",
  );
  assert(
    html.includes('href="http://localhost:4200"'),
    "The rendered page does not return to the local frontend.",
  );

  const stylesheetPath = html.match(/href="([^"]+\/css\/banking-login\.css)"/)?.[1];
  assert(stylesheetPath, "The rendered page does not include the banking stylesheet.");
  const stylesheetUrl = new URL(stylesheetPath, origin);
  const stylesheetResponse = await fetch(stylesheetUrl, {
    signal: AbortSignal.timeout(10_000),
  });
  assert(stylesheetResponse.ok, "The banking stylesheet is not available.");
  await stylesheetResponse.text();
  const fontResponse = await fetch(
    new URL("../fonts/geist-latin-wght-normal.woff2", stylesheetUrl),
    { signal: AbortSignal.timeout(10_000) },
  );
  assert(fontResponse.ok, "The self-hosted Geist font is not available.");
  await fontResponse.arrayBuffer();

  const adminTokenResponse = await fetch(
    new URL("/realms/master/protocol/openid-connect/token", origin),
    {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: "admin-cli",
        grant_type: "password",
        username: process.env.KEYCLOAK_ADMIN_USERNAME ?? "admin",
        password: process.env.KEYCLOAK_ADMIN_PASSWORD ?? "admin",
      }),
      signal: AbortSignal.timeout(10_000),
    },
  );
  assert(adminTokenResponse.ok, "The local Keycloak admin token could not be obtained.");
  const { access_token: adminAccessToken } = await adminTokenResponse.json();
  const liveRealmResponse = await fetch(
    new URL("/admin/realms/banking-ecosystem", origin),
    {
      headers: { authorization: `Bearer ${adminAccessToken}` },
      signal: AbortSignal.timeout(10_000),
    },
  );
  assert(liveRealmResponse.ok, "The live banking realm could not be read.");
  const liveRealm = await liveRealmResponse.json();
  assert(liveRealm.loginTheme === "banking", "The live login theme is not reconciled.");
  assert(liveRealm.emailTheme === "banking", "The live email theme is not reconciled.");
  assert(
    liveRealm.passwordPolicy === expectedPasswordPolicy,
    "The live password policy is not reconciled.",
  );
  assert(
    liveRealm.smtpServer?.fromDisplayName === "Nerva Banking",
    "The live SMTP sender display name is not reconciled.",
  );
  const liveRequiredActionsResponse = await fetch(
    new URL("/admin/realms/banking-ecosystem/authentication/required-actions", origin),
    {
      headers: { authorization: `Bearer ${adminAccessToken}` },
      signal: AbortSignal.timeout(10_000),
    },
  );
  assert(liveRequiredActionsResponse.ok, "The live required actions could not be read.");
  const liveRequiredActions = await liveRequiredActionsResponse.json();
  for (const [alias, priority] of Object.entries(expectedCredentialActionPriorities)) {
    const requiredAction = liveRequiredActions.find((candidate) => candidate.alias === alias);
    assert(
      requiredAction?.enabled === true && requiredAction.priority === priority,
      `The live ${alias} required action priority is not reconciled.`,
    );
  }
  const liveClientsResponse = await fetch(
    new URL("/admin/realms/banking-ecosystem/clients?clientId=home-banking-bff", origin),
    {
      headers: { authorization: `Bearer ${adminAccessToken}` },
      signal: AbortSignal.timeout(10_000),
    },
  );
  assert(liveClientsResponse.ok, "The live browser client could not be read.");
  const liveClients = await liveClientsResponse.json();
  assert(liveClients.length === 1, "The live browser client is missing or duplicated.");
  assert(
    JSON.stringify([...liveClients[0].redirectUris].sort()) ===
      JSON.stringify([...expectedLiveRedirectUris].sort()),
    "The live browser redirect URI contract is not reconciled.",
  );
}

console.log(`Theme verification passed${process.argv.includes("--live") ? " (live)" : ""}.`);
