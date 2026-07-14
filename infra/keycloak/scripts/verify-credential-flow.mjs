const keycloakOrigin = (process.env.KEYCLOAK_URL ?? "http://localhost:8090").replace(/\/$/, "");
const mailpitOrigin = (process.env.MAILPIT_URL ?? "http://localhost:8025").replace(/\/$/, "");
const realm = "banking-ecosystem";
const keycloakPublicBase = new URL(
  `${(process.env.KEYCLOAK_PUBLIC_URL ?? keycloakOrigin).replace(/\/$/, "")}/`,
);
const expectedIssuer = new URL(`realms/${realm}`, keycloakPublicBase)
  .toString()
  .replace(/\/$/, "");
const expectedLocalSmtp = {
  host: process.env.KEYCLOAK_LOCAL_SMTP_HOST ?? "host.docker.internal",
  port: process.env.KEYCLOAK_LOCAL_SMTP_PORT ?? "1025",
  from: process.env.KEYCLOAK_LOCAL_SMTP_FROM ?? "no-reply@nerva.local",
};
const completionEntryPoint =
  "http://localhost:8085/web/auth/login/onboarding-completion";
const expectedPasswordPolicy =
  "length(15) and maxLength(64) and notUsername and notEmail and passwordBlacklist(nerva-passwords.txt)";

const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

const decodeHtmlAttribute = (value) =>
  value
    .replaceAll("&amp;", "&")
    .replaceAll("&quot;", '"')
    .replaceAll("&#39;", "'")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">");

const formAction = (html, formId) => {
  const formTag = html.match(
    new RegExp(`<form\\b(?=[^>]*\\bid="${formId}")[^>]*>`, "i"),
  )?.[0];
  const action = formTag?.match(/\baction="([^"]+)"/i)?.[1];
  assert(action, `The ${formId} action URL is missing.`);
  return decodeHtmlAttribute(action);
};

const primaryAction = (html) => {
  const anchorTag = html.match(
    /<a\b(?=[^>]*\bclass="[^"]*banking-action-button[^"]*")[^>]*>/i,
  )?.[0];
  const href = anchorTag?.match(/\bhref="([^"]+)"/i)?.[1];
  assert(href, "The Keycloak action confirmation URL is missing.");
  return decodeHtmlAttribute(href);
};

const pageDiagnostics = (html, url, status) => {
  const title = html.match(/<title>([^<]*)<\/title>/i)?.[1]?.trim() ?? "no title";
  const heading = html
    .match(/<h1\b[^>]*>([\s\S]*?)<\/h1>/i)?.[1]
    ?.replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim() ?? "no heading";
  const forms = [...html.matchAll(/<form\b[^>]*\bid="([^"]+)"/gi)].map((match) => match[1]);
  const paragraphs = [...html.matchAll(/<p\b[^>]*>([\s\S]*?)<\/p>/gi)]
    .map((match) => match[1].replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim())
    .filter(Boolean)
    .slice(0, 6)
    .join(" | ");
  const actionHref = html
    .match(/<a\b(?=[^>]*\bclass="[^"]*banking-action-button[^"]*")[^>]*\bhref="([^"]+)"/i)?.[1];
  const actionTarget = actionHref
    ? (() => {
        const parsed = new URL(decodeHtmlAttribute(actionHref), keycloakOrigin);
        return `${parsed.origin}${parsed.pathname}`;
      })()
    : "none";
  const parsedUrl = new URL(url);
  const safeUrl = `${parsedUrl.origin}${parsedUrl.pathname}${parsedUrl.search ? "?redacted" : ""}`;
  return `status=${status}, url=${safeUrl}, title=${title}, heading=${heading}, action=${actionTarget}, paragraphs=${paragraphs || "none"}, forms=${forms.join(",") || "none"}`;
};

class CookieSession {
  cookies = new Map();

  absorb(response) {
    const values = response.headers.getSetCookie?.() ?? [];
    for (const value of values) {
      const pair = value.split(";", 1)[0];
      const separator = pair.indexOf("=");
      if (separator <= 0) continue;
      const name = pair.slice(0, separator);
      const cookieValue = pair.slice(separator + 1);
      if (cookieValue) this.cookies.set(name, cookieValue);
      else this.cookies.delete(name);
    }
  }

  header() {
    return [...this.cookies].map(([name, value]) => `${name}=${value}`).join("; ");
  }

  async follow(url, options = {}, stopAtExternalRedirect = false) {
    let currentUrl = new URL(url, keycloakOrigin);
    let method = options.method ?? "GET";
    let body = options.body;
    const baseHeaders = new Headers(options.headers);

    for (let redirects = 0; redirects < 12; redirects += 1) {
      const headers = new Headers(baseHeaders);
      const cookie = this.header();
      if (cookie) headers.set("cookie", cookie);
      if (method === "GET") headers.delete("content-type");

      const response = await fetch(currentUrl, {
        method,
        body: method === "GET" ? undefined : body,
        headers,
        redirect: "manual",
        signal: AbortSignal.timeout(10_000),
      });
      this.absorb(response);

      if (![301, 302, 303, 307, 308].includes(response.status)) {
        return { response, url: currentUrl, externalRedirect: null };
      }

      const location = response.headers.get("location");
      assert(location, "Keycloak returned a redirect without a location.");
      const nextUrl = new URL(location, currentUrl);
      if (stopAtExternalRedirect && nextUrl.origin !== new URL(keycloakOrigin).origin) {
        return { response, url: currentUrl, externalRedirect: nextUrl };
      }

      if (response.status === 303 || ([301, 302].includes(response.status) && method === "POST")) {
        method = "GET";
        body = undefined;
      }
      currentUrl = nextUrl;
    }

    throw new Error("Keycloak exceeded the redirect limit.");
  }
}

const discoveryUrl = `${keycloakOrigin}/realms/${realm}/.well-known/openid-configuration`;
const discoveryResponse = await fetch(discoveryUrl, {
  signal: AbortSignal.timeout(10_000),
});
assert(discoveryResponse.ok, "The local Keycloak discovery document could not be read.");
const discovery = await discoveryResponse.json();

const hostileHostDiscoveryResponse = await fetch(discoveryUrl, {
  headers: { host: "attacker.invalid" },
  signal: AbortSignal.timeout(10_000),
});
assert(
  hostileHostDiscoveryResponse.ok,
  "Keycloak rejected the hostname stability verification request.",
);
const hostileHostDiscovery = await hostileHostDiscoveryResponse.json();
assert(
  discovery.issuer === expectedIssuer && hostileHostDiscovery.issuer === expectedIssuer,
  "Keycloak issuer metadata changes with the request Host header.",
);

const tokenResponse = await fetch(
  `${keycloakOrigin}/realms/master/protocol/openid-connect/token`,
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
assert(tokenResponse.ok, "The local Keycloak admin token could not be obtained.");
const { access_token: adminAccessToken } = await tokenResponse.json();

const adminFetch = (path, options = {}) => {
  const headers = new Headers(options.headers);
  headers.set("authorization", `Bearer ${adminAccessToken}`);
  return fetch(`${keycloakOrigin}${path}`, {
    ...options,
    headers,
    signal: AbortSignal.timeout(10_000),
  });
};

const stamp = crypto.randomUUID().replaceAll("-", "").slice(0, 12);
const provisionalUsername = `pending-theme-${stamp}`;
const chosenUsername = `theme.${stamp}`;
const email = `keycloak-step4-${stamp}@example.com`;
let userId;

try {
  const liveRealmResponse = await adminFetch(`/admin/realms/${realm}`);
  assert(liveRealmResponse.ok, "The live realm could not be read.");
  const liveRealm = await liveRealmResponse.json();
  assert(liveRealm.emailTheme === "banking", "The live email theme is not banking.");
  assert(
    liveRealm.passwordPolicy === expectedPasswordPolicy,
    "The live password policy does not match the credential journey.",
  );
  const liveSmtp = liveRealm.smtpServer ?? {};
  assert(
    liveSmtp.host === expectedLocalSmtp.host &&
      String(liveSmtp.port) === expectedLocalSmtp.port &&
      liveSmtp.from === expectedLocalSmtp.from &&
      liveSmtp.fromDisplayName === "Nerva Banking" &&
      String(liveSmtp.auth).toLowerCase() === "false" &&
      String(liveSmtp.starttls).toLowerCase() === "false" &&
      String(liveSmtp.ssl).toLowerCase() === "false" &&
      !String(liveSmtp.user ?? "").trim() &&
      !String(liveSmtp.password ?? "").trim(),
    "The live realm is not using the credential-free local Mailpit contract.",
  );

  const createResponse = await adminFetch(`/admin/realms/${realm}/users`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      username: provisionalUsername,
      email,
      firstName: "Ana",
      lastName: "Prueba",
      enabled: true,
      emailVerified: true,
      requiredActions: ["UPDATE_PROFILE", "UPDATE_PASSWORD"],
    }),
  });
  assert(createResponse.status === 201, "The disposable Keycloak user could not be created.");
  userId = createResponse.headers.get("location")?.split("/").at(-1);
  assert(userId, "Keycloak did not return the disposable user location.");

  const actionEmailUrl = new URL(
    `${keycloakOrigin}/admin/realms/${realm}/users/${userId}/execute-actions-email`,
  );
  actionEmailUrl.search = new URLSearchParams({
    client_id: "home-banking-bff",
    redirect_uri: completionEntryPoint,
    lifespan: "3600",
  });
  const actionEmailResponse = await fetch(actionEmailUrl, {
    method: "PUT",
    headers: {
      authorization: `Bearer ${adminAccessToken}`,
      "content-type": "application/json",
    },
    body: JSON.stringify(["UPDATE_PROFILE", "UPDATE_PASSWORD"]),
    signal: AbortSignal.timeout(10_000),
  });
  assert(actionEmailResponse.status === 204, "Keycloak did not accept the credential email.");

  let message;
  const emailDeadline = Date.now() + 20_000;
  do {
    const messagesResponse = await fetch(`${mailpitOrigin}/api/v1/messages?limit=100`, {
      signal: AbortSignal.timeout(10_000),
    });
    assert(messagesResponse.ok, "Mailpit messages could not be read.");
    const messages = (await messagesResponse.json()).messages;
    message = messages.find((candidate) =>
      candidate.To?.some((recipient) => recipient.Address === email),
    );
    if (!message) await new Promise((resolve) => setTimeout(resolve, 500));
  } while (!message && Date.now() < emailDeadline);
  assert(message, "The credential email did not arrive in Mailpit.");
  assert(
    message.Subject === "Completá tu acceso de prueba a Nerva Banking",
    "The credential email subject is not the approved Spanish copy.",
  );
  assert(message.From?.Name === "Nerva Banking", "The email sender has no Nerva display name.");

  const messageResponse = await fetch(`${mailpitOrigin}/api/v1/message/${message.ID}`, {
    signal: AbortSignal.timeout(10_000),
  });
  assert(messageResponse.ok, "The Mailpit credential email could not be read.");
  const messageDetail = await messageResponse.json();
  const html = String(messageDetail.HTML ?? "");
  const text = String(messageDetail.Text ?? "");
  for (const body of [html, text]) {
    assert(body.includes("Creá tu acceso"), "The credential email title is missing.");
    assert(
      body.includes("Nerva Banking no es una entidad financiera"),
      "The academic email disclaimer is missing.",
    );
    assert(
      !/(administrador|actualizar perfil|UPDATE_PROFILE|UPDATE_PASSWORD|Keycloak)/i.test(body),
      "The credential email exposes generic administration or implementation copy.",
    );
  }

  const actionLinkMatch = html.match(
    /href="([^"]*\/realms\/banking-ecosystem\/login-actions\/action-token[^"]*)"/,
  );
  assert(actionLinkMatch, "The credential action link is missing from the HTML email.");
  const actionLink = decodeHtmlAttribute(actionLinkMatch[1]);
  const actionUrl = new URL(actionLink);
  const expectedActionPath = new URL(
    `realms/${realm}/login-actions/action-token`,
    keycloakPublicBase,
  ).pathname;
  assert(
    actionUrl.origin === keycloakPublicBase.origin &&
      actionUrl.pathname === expectedActionPath &&
      actionUrl.searchParams.has("key") &&
      !actionUrl.hash,
    "The credential email action link does not use the configured Keycloak public URL.",
  );

  const session = new CookieSession();
  let usernamePageResult = await session.follow(actionLink);
  assert(usernamePageResult.response.ok, "The username action page did not render.");
  let page = await usernamePageResult.response.text();
  if (!page.includes('id="kc-update-profile-form"') && page.includes('id="kc-info-message"')) {
    assert(
      page.includes("Creá tu acceso") &&
        page.includes("Primero elegí tu usuario. Después creá una contraseña.") &&
        page.includes(">Empezar<") &&
        !page.includes("Realice las siguientes acciones"),
      "The credential introduction still exposes generic Keycloak copy.",
    );
    usernamePageResult = await session.follow(primaryAction(page));
    assert(usernamePageResult.response.ok, "The confirmed username action page did not render.");
    page = await usernamePageResult.response.text();
  }
  assert(
    page.includes('id="kc-update-profile-form"'),
    `The Nerva username form did not render (${pageDiagnostics(page, usernamePageResult.url, usernamePageResult.response.status)}).`,
  );
  assert(page.includes("Elegí tu usuario"), "The username page copy is not in Spanish.");
  assert(
    !page.includes("Elegí tu usuario para continuar."),
    "The username page repeats a non-actionable global prompt.",
  );
  assert(!page.includes('name="email"'), "The username page exposes the applicant email field.");
  assert(
    !page.includes(provisionalUsername) && /name="username"[\s\S]*?value=""/.test(page),
    "The username page exposes its internal provisional identifier.",
  );

  const invalidUsernameResponse = await session.follow(formAction(page, "kc-update-profile-form"), {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ username: "a" }),
  });
  assert(invalidUsernameResponse.response.ok, "The invalid username response did not render.");
  page = await invalidUsernameResponse.response.text();
  assert(
    page.includes('id="kc-update-profile-form"') &&
      page.includes('value="a"') &&
      page.includes("Usá entre 4 y 64 caracteres."),
    "The username validation does not preserve the attempted value and its customer-facing error.",
  );

  const usernameResponse = await session.follow(formAction(page, "kc-update-profile-form"), {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ username: chosenUsername }),
  });
  assert(usernameResponse.response.ok, "The username action could not continue.");
  page = await usernameResponse.response.text();
  assert(page.includes('id="kc-passwd-update-form"'), "The Nerva password form did not render.");
  assert(page.includes("Creá tu contraseña"), "The password page copy is not in Spanish.");
  assert(
    page.includes("Usá entre 15 y 64 caracteres. Podés incluir espacios."),
    "The password page does not explain or enforce its supported length range.",
  );
  assert(
    !page.includes("Creá tu contraseña para continuar."),
    "The password page repeats a non-actionable global prompt.",
  );

  const shortPasswordResponse = await session.follow(formAction(page, "kc-passwd-update-form"), {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      "password-new": "muy-corta",
      "password-confirm": "muy-corta",
    }),
  });
  page = await shortPasswordResponse.response.text();
  assert(page.includes("Usá al menos 15 caracteres."), "The minimum-length error is not actionable.");

  const longPassword = "una frase de prueba deliberadamente extensa ".repeat(2);
  const longPasswordResponse = await session.follow(formAction(page, "kc-passwd-update-form"), {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      "password-new": longPassword,
      "password-confirm": longPassword,
    }),
  });
  page = await longPasswordResponse.response.text();
  assert(page.includes("Usá hasta 64 caracteres."), "The maximum-length error is not actionable.");

  const blockedPassword = "nerva banking 2026";
  const blockedPasswordResponse = await session.follow(formAction(page, "kc-passwd-update-form"), {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      "password-new": blockedPassword,
      "password-confirm": blockedPassword,
    }),
  });
  page = await blockedPasswordResponse.response.text();
  assert(
    page.includes("Esa contraseña es demasiado común. Elegí otra distinta."),
    "The blocklist error is not actionable.",
  );

  const validPassphrase = "esta es una frase local distinta 2026";
  const completionResult = await session.follow(
    formAction(page, "kc-passwd-update-form"),
    {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        "password-new": validPassphrase,
        "password-confirm": validPassphrase,
      }),
    },
    true,
  );
  const expectedCompletionUrl = new URL(completionEntryPoint);
  let completionRedirect = completionResult.externalRedirect;
  if (!completionRedirect) {
    assert(completionResult.response.ok, "The credential completion page did not render.");
    const completionPage = await completionResult.response.text();
    assert(
      completionPage.includes('id="kc-info-message"') &&
        completionPage.includes("Acceso creado") &&
        completionPage.includes("Continuá para terminar de preparar tu cuenta.") &&
        !completionPage.includes("Cuenta actualizada"),
      `The credential completion page is not the approved Nerva screen (${pageDiagnostics(
        completionPage,
        completionResult.url,
        completionResult.response.status,
      )}).`,
    );
    completionRedirect = new URL(primaryAction(completionPage), keycloakOrigin);
  }
  assert(
    completionRedirect?.origin === expectedCompletionUrl.origin &&
      completionRedirect.pathname === expectedCompletionUrl.pathname &&
      completionRedirect.hash === "",
    `Credential completion did not return to the principal-derived BFF entry point (target=${
      completionRedirect ? `${completionRedirect.origin}${completionRedirect.pathname}` : "none"
    }).`,
  );

  const reusedSession = new CookieSession();
  const reusedActionResult = await reusedSession.follow(actionLink);
  const reusedActionPage = await reusedActionResult.response.text();
  assert(
    reusedActionPage.includes("Este enlace ya no está disponible") &&
      reusedActionPage.includes(
        "Si todavía no creaste tu acceso, pedí un nuevo correo desde el seguimiento de tu solicitud.",
      ),
    "A used credential link does not expose the approved recovery copy.",
  );

  const userResponse = await adminFetch(`/admin/realms/${realm}/users/${userId}`);
  assert(userResponse.ok, "The completed disposable user could not be read.");
  const completedUser = await userResponse.json();
  assert(completedUser.username === chosenUsername, "The chosen username was not persisted.");
  assert(
    completedUser.email === email &&
      completedUser.firstName === "Ana" &&
      completedUser.lastName === "Prueba",
    "The username action did not preserve the applicant identity attributes.",
  );
  assert(
    !completedUser.requiredActions?.includes("UPDATE_PROFILE") &&
      !completedUser.requiredActions?.includes("UPDATE_PASSWORD"),
    "Credential required actions remain after completion.",
  );

  const credentialsResponse = await adminFetch(
    `/admin/realms/${realm}/users/${userId}/credentials`,
  );
  assert(credentialsResponse.ok, "The completed user credentials could not be read.");
  const credentials = await credentialsResponse.json();
  assert(
    credentials.some((credential) => credential.type === "password"),
    "The valid passphrase was not stored by Keycloak.",
  );

  console.log("Credential theme flow verification passed (email, username, password and redirect).");
} finally {
  if (userId) {
    const deleteResponse = await adminFetch(`/admin/realms/${realm}/users/${userId}`, {
      method: "DELETE",
    });
    if (![204, 404].includes(deleteResponse.status)) {
      console.error("Warning: the disposable Keycloak user could not be deleted.");
    }
  }
}
