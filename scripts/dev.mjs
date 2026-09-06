import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

// The supervised QA runner uses Vite flags. Keep Next.js and translate only
// those aliases; regular npm run dev arguments continue to work unchanged.
const args = process.argv.slice(2).filter(arg => arg !== "--strictPort").map(arg => arg === "--host" ? "--hostname" : arg);
const child = spawn(process.execPath, [fileURLToPath(new URL("../node_modules/next/dist/bin/next", import.meta.url)), "dev", ...args], {
  stdio: "inherit", env: { ...process.env, NEXT_TELEMETRY_DISABLED: "1" },
});
for (const signal of ["SIGTERM", "SIGINT"]) process.on(signal, () => child.kill(signal));
child.on("exit", code => process.exit(code ?? 1));
child.on("error", error => { console.error(error.message); process.exit(1); });
