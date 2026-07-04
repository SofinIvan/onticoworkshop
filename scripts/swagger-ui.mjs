import { createServer } from "node:http";
import { createReadStream, existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const projectRoot = process.cwd();
const swaggerUiRoot = path.dirname(require.resolve("swagger-ui-dist/package.json"));
const openApiPath = path.join(projectRoot, "tsp-output", "schema", "openapi.yaml");
const port = Number.parseInt(process.env.PORT ?? "8080", 10);
const host = process.env.HOST ?? "127.0.0.1";

const mimeTypes = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".map": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".yaml": "application/yaml; charset=utf-8",
  ".yml": "application/yaml; charset=utf-8",
};

function send(res, statusCode, body, contentType = "text/plain; charset=utf-8") {
  res.writeHead(statusCode, { "content-type": contentType });
  res.end(body);
}

function safeSwaggerAssetPath(urlPath) {
  const assetPath = path.normalize(urlPath.replace(/^\/+/, ""));
  if (assetPath.startsWith("..") || path.isAbsolute(assetPath)) {
    return undefined;
  }

  return path.join(swaggerUiRoot, assetPath);
}

async function renderIndex() {
  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Calendar Booking API - Swagger UI</title>
    <link rel="stylesheet" href="/swagger-ui.css" />
    <link rel="icon" href="/favicon-32x32.png" />
    <style>
      body { margin: 0; background: #ffffff; }
      .swagger-ui .topbar { display: none; }
    </style>
  </head>
  <body>
    <div id="swagger-ui"></div>
    <script src="/swagger-ui-bundle.js"></script>
    <script src="/swagger-ui-standalone-preset.js"></script>
    <script>
      window.ui = SwaggerUIBundle({
        url: "/openapi.yaml",
        dom_id: "#swagger-ui",
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        layout: "StandaloneLayout",
        tryItOutEnabled: true,
        displayRequestDuration: true
      });
    </script>
  </body>
</html>`;
}

const server = createServer(async (req, res) => {
  const requestUrl = new URL(req.url ?? "/", `http://${host}:${port}`);

  if (requestUrl.pathname === "/" || requestUrl.pathname === "/index.html") {
    send(res, 200, await renderIndex(), "text/html; charset=utf-8");
    return;
  }

  if (requestUrl.pathname === "/openapi.yaml") {
    if (!existsSync(openApiPath)) {
      send(res, 404, "OpenAPI file not found. Run `npm run api:build` first.");
      return;
    }

    res.writeHead(200, { "content-type": mimeTypes[".yaml"] });
    createReadStream(openApiPath).pipe(res);
    return;
  }

  const assetPath = safeSwaggerAssetPath(requestUrl.pathname);
  if (assetPath && existsSync(assetPath)) {
    const extension = path.extname(assetPath);
    res.writeHead(200, { "content-type": mimeTypes[extension] ?? "application/octet-stream" });
    createReadStream(assetPath).pipe(res);
    return;
  }

  send(res, 404, "Not found");
});

server.listen(port, host, async () => {
  const packageJson = JSON.parse(await readFile(path.join(projectRoot, "package.json"), "utf8"));
  console.log(`${packageJson.name} Swagger UI: http://${host}:${port}`);
  console.log(`OpenAPI source: ${path.relative(projectRoot, openApiPath)}`);
});
