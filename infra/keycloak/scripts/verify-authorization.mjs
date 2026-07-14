import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const keycloakDirectory = path.resolve(scriptDirectory, "..");
const realmName = "banking-ecosystem";
const accountClientId = "account-service";

const managedRealmRoles = [
  "HOME_BANKING_USER",
  "CUSTOMER_READ",
  "CUSTOMER_WRITE",
  "CUSTOMER_PROVISION",
  "ACCOUNT_READ",
  "ACCOUNT_WRITE",
  "ACCOUNT_PROVISION",
  "IDENTITY_READ",
  "IDENTITY_WRITE",
  "IDENTITY_PROVISION",
  "NOTIFICATION_WRITE",
  "DOCUMENT_READ",
  "DOCUMENT_WRITE",
  "ONBOARDING_READ",
  "ONBOARDING_WRITE",
  "ONBOARDING_OPERATE",
];

const expectedRolesByUsername = {
  "service-account-onboarding-bff-service": ["ONBOARDING_READ", "ONBOARDING_WRITE"],
  "service-account-home-banking-bff-service": [
    "CUSTOMER_READ",
    "ACCOUNT_READ",
    "IDENTITY_READ",
  ],
  "service-account-onboarding-orchestrator": [
    "CUSTOMER_READ",
    "CUSTOMER_PROVISION",
    "ACCOUNT_READ",
    "ACCOUNT_PROVISION",
    "IDENTITY_READ",
    "IDENTITY_PROVISION",
    "DOCUMENT_READ",
    "DOCUMENT_WRITE",
    "NOTIFICATION_WRITE",
  ],
  "service-account-account-service": ["CUSTOMER_READ"],
  "banking-admin": [
    "CUSTOMER_READ",
    "CUSTOMER_WRITE",
    "CUSTOMER_PROVISION",
    "ACCOUNT_READ",
    "ACCOUNT_WRITE",
    "ACCOUNT_PROVISION",
    "IDENTITY_READ",
    "IDENTITY_WRITE",
    "IDENTITY_PROVISION",
    "NOTIFICATION_WRITE",
    "DOCUMENT_READ",
    "DOCUMENT_WRITE",
    "ONBOARDING_READ",
    "ONBOARDING_WRITE",
    "ONBOARDING_OPERATE",
  ],
  "home-banking-user": ["HOME_BANKING_USER"],
};

const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

const sorted = (values) => [...values].sort();
const sameValues = (actual, expected) =>
  JSON.stringify(sorted(actual)) === JSON.stringify(sorted(expected));
const managedOnly = (roles) => roles.filter((role) => managedRealmRoles.includes(role));

const realm = JSON.parse(
  await readFile(path.join(keycloakDirectory, "realms", "banking-ecosystem-realm.json"), "utf8"),
);
const compose = await readFile(path.join(keycloakDirectory, "docker-compose.yml"), "utf8");
const envExample = await readFile(path.join(keycloakDirectory, ".env.example"), "utf8");

const declaredRoleNames = realm.roles?.realm?.map((role) => role.name) ?? [];
for (const role of [
  "CUSTOMER_PROVISION",
  "ACCOUNT_PROVISION",
  "IDENTITY_PROVISION",
  "ONBOARDING_OPERATE",
]) {
  assert(declaredRoleNames.includes(role), `The realm import is missing ${role}.`);
}

const accountClient = realm.clients?.find((client) => client.clientId === accountClientId);
assert(accountClient, "The realm import is missing the account-service client.");
assert(
  accountClient.enabled === true &&
    accountClient.protocol === "openid-connect" &&
    accountClient.publicClient === false &&
    accountClient.clientAuthenticatorType === "client-secret" &&
    accountClient.standardFlowEnabled === false &&
    accountClient.implicitFlowEnabled === false &&
    accountClient.directAccessGrantsEnabled === false &&
    accountClient.serviceAccountsEnabled === true,
  "The account-service client is not restricted to client credentials.",
);
assert(
  accountClient.secret === "${ACCOUNT_INTERNAL_OAUTH_CLIENT_SECRET}",
  "The account-service client secret is not environment-backed.",
);

for (const [username, expectedRoles] of Object.entries(expectedRolesByUsername)) {
  const user = realm.users?.find((candidate) => candidate.username === username);
  assert(user, `The realm import is missing ${username}.`);
  assert(
    sameValues(managedOnly(user.realmRoles ?? []), expectedRoles),
    `${username} does not have the exact application-role set.`,
  );
}

const orchestrator = expectedRolesByUsername["service-account-onboarding-orchestrator"];
assert(
  !orchestrator.some((role) =>
    ["CUSTOMER_WRITE", "ACCOUNT_WRITE", "IDENTITY_WRITE", "ONBOARDING_OPERATE"].includes(role),
  ),
  "The onboarding orchestrator still has a broad write or operator role.",
);
assert(
  !expectedRolesByUsername["service-account-onboarding-bff-service"].includes(
    "ONBOARDING_OPERATE",
  ),
  "The browser-facing onboarding service account has the operator role.",
);

assert(
  (compose.match(/ACCOUNT_INTERNAL_OAUTH_CLIENT_SECRET/g) ?? []).length >= 3 &&
    compose.includes("ensure_realm_role CUSTOMER_PROVISION") &&
    compose.includes("ensure_realm_role ACCOUNT_PROVISION") &&
    compose.includes("ensure_realm_role IDENTITY_PROVISION") &&
    compose.includes("ensure_realm_role ONBOARDING_OPERATE") &&
    compose.includes("ensure_service_client account-service") &&
    compose.includes("reconcile_exact_realm_roles service-account-account-service CUSTOMER_READ"),
  "The existing-volume initializer does not reconcile the authorization contract.",
);
assert(
  /^ACCOUNT_INTERNAL_OAUTH_CLIENT_SECRET=local-account-secret$/m.test(envExample),
  "The local account-service client secret is missing from .env.example.",
);

if (process.argv.includes("--live")) {
  const origin = process.env.KEYCLOAK_ORIGIN ?? "http://localhost:8090";
  const request = async (url, options = {}) =>
    fetch(url, { ...options, signal: AbortSignal.timeout(10_000) });
  const tokenResponse = await request(
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
    },
  );
  assert(tokenResponse.ok, "The local Keycloak admin token could not be obtained.");
  const { access_token: adminToken } = await tokenResponse.json();
  const adminFetch = (pathname) =>
    request(new URL(pathname, origin), {
      headers: { authorization: `Bearer ${adminToken}` },
    });

  const liveRolesResponse = await adminFetch(`/admin/realms/${realmName}/roles`);
  assert(liveRolesResponse.ok, "The live realm roles could not be read.");
  const liveRoleNames = (await liveRolesResponse.json()).map((role) => role.name);
  for (const role of declaredRoleNames) {
    assert(liveRoleNames.includes(role), `The live realm is missing ${role}.`);
  }

  for (const [username, expectedRoles] of Object.entries(expectedRolesByUsername)) {
    const usersResponse = await adminFetch(
      `/admin/realms/${realmName}/users?exact=true&username=${encodeURIComponent(username)}`,
    );
    assert(usersResponse.ok, `The live ${username} user could not be read.`);
    const users = await usersResponse.json();
    assert(users.length === 1, `The live ${username} user is missing or duplicated.`);
    const mappingsResponse = await adminFetch(
      `/admin/realms/${realmName}/users/${users[0].id}/role-mappings/realm`,
    );
    assert(mappingsResponse.ok, `The live ${username} role mappings could not be read.`);
    const actualRoles = (await mappingsResponse.json()).map((role) => role.name);
    assert(
      sameValues(managedOnly(actualRoles), expectedRoles),
      `The live ${username} application-role set was not reconciled.`,
    );
  }

  const clientsResponse = await adminFetch(
    `/admin/realms/${realmName}/clients?clientId=${encodeURIComponent(accountClientId)}`,
  );
  assert(clientsResponse.ok, "The live account-service client could not be read.");
  const liveClients = await clientsResponse.json();
  assert(liveClients.length === 1, "The live account-service client is missing or duplicated.");
  const liveAccountClient = liveClients[0];
  assert(
    liveAccountClient.enabled === true &&
      liveAccountClient.protocol === "openid-connect" &&
      liveAccountClient.publicClient === false &&
      liveAccountClient.clientAuthenticatorType === "client-secret" &&
      liveAccountClient.standardFlowEnabled === false &&
      liveAccountClient.implicitFlowEnabled === false &&
      liveAccountClient.directAccessGrantsEnabled === false &&
      liveAccountClient.serviceAccountsEnabled === true,
    "The live account-service client is not restricted to client credentials.",
  );

  const accountTokenResponse = await request(
    new URL(`/realms/${realmName}/protocol/openid-connect/token`, origin),
    {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: accountClientId,
        client_secret:
          process.env.ACCOUNT_INTERNAL_OAUTH_CLIENT_SECRET ?? "local-account-secret",
        grant_type: "client_credentials",
      }),
    },
  );
  assert(accountTokenResponse.ok, "The account-service client could not obtain a token.");
  const { access_token: accountToken } = await accountTokenResponse.json();
  const payload = JSON.parse(Buffer.from(accountToken.split(".")[1], "base64url").toString("utf8"));
  assert(
    sameValues(managedOnly(payload.realm_access?.roles ?? []), ["CUSTOMER_READ"]),
    "The account-service token contains roles beyond CUSTOMER_READ.",
  );
}

console.log(
  `Keycloak authorization verification passed${process.argv.includes("--live") ? " (live)" : ""}.`,
);
