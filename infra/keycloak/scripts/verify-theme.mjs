import { access, readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryDirectory = path.resolve(scriptDirectory, "..", "..", "..");
const keycloakDirectory = path.join(repositoryDirectory, "infra", "keycloak");
const loginThemeDirectory = path.join(keycloakDirectory, "themes", "banking", "login");

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

assert(realm.loginTheme === "banking", "The banking login theme is not selected.");
assert(realm.defaultLocale === "es", "Spanish is not the default realm locale.");
assert(
  JSON.stringify(realm.supportedLocales) === JSON.stringify(["es"]),
  "The customer realm must support Spanish only.",
);
assert(/^locales=es$/m.test(themeProperties), "The login theme must expose Spanish only.");
assert(
  themeProperties.includes("${env.BANKING_FRONTEND_URL\\:http://localhost:4200}"),
  "The frontend return URL is not environment-aware.",
);

const customerFacingSources = `${template}\n${login}\n${messages}`;
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
}

console.log(`Theme verification passed${process.argv.includes("--live") ? " (live)" : ""}.`);
