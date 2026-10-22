import session from "express-session";
import MongoStore from "connect-mongo";

export function createSessionMiddleware(mongoClient) {
  const secret = process.env.SESSION_SECRET;

  if (!secret || secret.length < 32) {
    throw new Error("Configure a session secret of at least 32 characters.");
  }

  const store = MongoStore.create({
    client: mongoClient,
    dbName: process.env.MONGODB_DB,
    collectionName: "sessions",
    ttl: 30 * 60,
  });

  store.on("error", () => {
    console.error("Demo session storage is unavailable.");
  });

  return session({
    name: "hris_demo.sid",
    secret,
    store,
    resave: false,
    saveUninitialized: false,
    rolling: true,
    cookie: {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      maxAge: 30 * 60 * 1000,
      path: "/",
    },
  });
}