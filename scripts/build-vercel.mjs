import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const source = path.join(root, "src/main/resources/static");
const output = path.join(root, "dist");
const configuredApiUrl = process.env.WORKPLACE_API_URL?.trim();

if (!configuredApiUrl) {
    throw new Error("Set WORKPLACE_API_URL to the deployed Render API origin before building the Vercel site.");
}

let apiOrigin;
try {
    const parsedUrl = new URL(configuredApiUrl);
    if (!["https:", "http:"].includes(parsedUrl.protocol) ||
        (parsedUrl.protocol !== "https:" && !["localhost", "127.0.0.1"].includes(parsedUrl.hostname)) ||
        parsedUrl.pathname !== "/" || parsedUrl.search || parsedUrl.hash) {
        throw new Error();
    }
    apiOrigin = parsedUrl.origin;
} catch {
    throw new Error("WORKPLACE_API_URL must be an HTTPS origin without a path (HTTP is allowed for localhost only).");
}

await rm(output, { recursive: true, force: true });
await mkdir(output, { recursive: true });
await cp(source, output, { recursive: true });

const runtimeConfig = path.join(output, "runtime-config.js");
const configContents = `window.WORKPLACE_API_BASE = ${JSON.stringify(apiOrigin)};\n`;
await writeFile(runtimeConfig, configContents, "utf8");

const htmlFiles = ["index.html", "jobs.html", "job.html", "employers.html"];
for (const filename of htmlFiles) {
    const htmlPath = path.join(output, filename);
    const html = await readFile(htmlPath, "utf8");
    if (!html.includes('<script src="/runtime-config.js"></script>')) {
        throw new Error(`${filename} must load runtime-config.js before app.js.`);
    }
}

console.log(`Built Vercel static site for ${apiOrigin}`);
