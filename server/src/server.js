import express from "express";
import mongoose from "mongoose";
import { createSessionMiddleware } from "./config/session.js";
import { platformRouter } from "./routes/platformRoutes.js";

const app = express();

const host = process.env.HOST || "127.0.0.1";
const port = Number(process.env.PORT || 5001);
const mongoUri = process.env.MONGODB_URI;
const databaseName = process.env.MONGODB_DB;

app.disable("x-powered-by");
app.use(express.json({ limit: "16kb" }));

app.get("/api/health", (_request, response) => {
  const connected = mongoose.connection.readyState === 1;

  response
    .status(connected ? 200 : 503)
    .set("Cache-Control", "no-store")
    .json({
      status: connected ? "ok" : "unavailable",
      database: connected ? "connected" : "disconnected",
    });
});

async function start() {
  if (!mongoUri || !databaseName) {
    throw new Error("Missing MongoDB configuration.");
  }

  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error("Invalid server port.");
  }

  await mongoose.connect(mongoUri, {
    dbName: databaseName,
    serverSelectionTimeoutMS: 10000,
  });

  console.log("MongoDB connected.");
  app.use(
    createSessionMiddleware(mongoose.connection.getClient()),
  );

  app.use("/api", platformRouter);

app.use((error, _request, response, _next) => {
  const invalidJson = error.type === "entity.parse.failed";
  const oversizedBody = error.type === "entity.too.large";

  const status = invalidJson ? 400 : oversizedBody ? 413 : 500;

  if (status === 500) {
    console.error("Demo API request failed.");
  }

  response.status(status).json({
    message: invalidJson
      ? "Provide valid JSON."
      : oversizedBody
        ? "Request body is too large."
        : "Unable to complete the request.",
  });
});

  const server = app.listen(port, host, () => {
    console.log(`Demo API running at http://${host}:${port}`);
  });

  server.on("error", () => {
    console.error("Unable to start HTTP server. Check the configured port.");
    process.exit(1);
  });
}

start().catch((error) => {
  console.error(
    `Startup failed (${error.name}). Check server/.env, Atlas credentials and IP access.`,
  );
  process.exit(1);
});