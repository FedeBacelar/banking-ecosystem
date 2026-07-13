import { readFile, mkdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const designSystemDirectory = path.resolve(scriptDirectory, "..");
const repositoryDirectory = path.resolve(designSystemDirectory, "..");
const checkOnly = process.argv.includes("--check");

const tokens = JSON.parse(
  await readFile(path.join(designSystemDirectory, "tokens.json"), "utf8"),
);

const toKebabCase = (value) =>
  value.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);

const cssLines = [
  "/* Generated from design-system/tokens.json. Do not edit directly. */",
  ":root {",
];

for (const [group, values] of Object.entries(tokens)) {
  if (group.startsWith("$")) continue;

  for (const [name, value] of Object.entries(values)) {
    cssLines.push(`  --nerva-${toKebabCase(group)}-${toKebabCase(name)}: ${value};`);
  }
}

cssLines.push("}", "");

const generatedFiles = new Map([
  [
    path.join(designSystemDirectory, "generated", "nerva-tokens.css"),
    cssLines.join("\n"),
  ],
  [
    path.join(
      repositoryDirectory,
      "infra",
      "keycloak",
      "themes",
      "banking",
      "login",
      "resources",
      "css",
      "nerva-tokens.css",
    ),
    cssLines.join("\n"),
  ],
  [
    path.join(
      repositoryDirectory,
      "banking-web",
      "src",
      "styles",
      "_nerva-tokens.css",
    ),
    cssLines.join("\n"),
  ],
]);

const keycloakResourcesDirectory = path.join(
  repositoryDirectory,
  "infra",
  "keycloak",
  "themes",
  "banking",
  "login",
  "resources",
);
const angularPublicAssetsDirectory = path.join(
  repositoryDirectory,
  "banking-web",
  "public",
  "assets",
);

const assetCopies = [
  [
    "nerva-logo.svg",
    path.join(keycloakResourcesDirectory, "img", "banking-logo.svg"),
  ],
  [
    "nerva-logo-light.svg",
    path.join(keycloakResourcesDirectory, "img", "banking-logo-light.svg"),
  ],
  [
    "fonts/geist/geist-latin-wght-normal.woff2",
    path.join(keycloakResourcesDirectory, "fonts", "geist-latin-wght-normal.woff2"),
  ],
  [
    "nerva-logo.svg",
    path.join(angularPublicAssetsDirectory, "brand", "nerva-logo.svg"),
  ],
  [
    "nerva-logo-light.svg",
    path.join(angularPublicAssetsDirectory, "brand", "nerva-logo-light.svg"),
  ],
  [
    "fonts/geist/geist-latin-wght-normal.woff2",
    path.join(angularPublicAssetsDirectory, "fonts", "geist-latin-wght-normal.woff2"),
  ],
];

for (const [source, destination] of assetCopies) {
  const contents = await readFile(path.join(designSystemDirectory, "assets", source));
  generatedFiles.set(destination, contents);
}

let hasDifferences = false;

for (const [destination, expectedContents] of generatedFiles) {
  if (checkOnly) {
    const currentContents = await readFile(destination).catch(() => undefined);
    const expectedBuffer = Buffer.isBuffer(expectedContents)
      ? expectedContents
      : Buffer.from(expectedContents);

    if (!currentContents?.equals(expectedBuffer)) {
      hasDifferences = true;
      console.error(`Desactualizado: ${path.relative(repositoryDirectory, destination)}`);
    }

    continue;
  }

  await mkdir(path.dirname(destination), { recursive: true });
  await writeFile(destination, expectedContents);
  console.log(`Generado: ${path.relative(repositoryDirectory, destination)}`);
}

if (hasDifferences) process.exitCode = 1;
