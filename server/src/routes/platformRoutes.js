import express from "express";

import { Employee } from "../models/Employee.js";
import { verifyPassword } from "../lib/password.js";

export const platformRouter = express.Router();

platformRouter.use((request, response, next) => {
  response.set("Cache-Control", "no-store");

  const changesState = !["GET", "HEAD", "OPTIONS"].includes(
    request.method,
  );

  if (
    changesState &&
    request.get("Origin") !== process.env.FRONTEND_ORIGIN
  ) {
    return response.status(403).json({
      message: "Request origin is not permitted.",
    });
  }

  next();
});

function publicEmployee(employee) {
  return {
    employeeId: employee.employeeId,
    firstName: employee.firstName,
    lastName: employee.lastName,
    email: employee.email,
    designation: employee.designation,
  };
}

export async function requireEmployeeSession(
  request,
  response,
  next,
) {
  const employeeId = request.session.employeeId;

  if (
    typeof employeeId !== "string" ||
    !/^[a-f0-9]{24}$/i.test(employeeId)
  ) {
    return response.status(401).json({
      message: "Please sign in to the demo platform.",
    });
  }

  const employee = await Employee.findOne({
    _id: employeeId,
    active: true,
  });

  if (!employee) {
    return response.status(401).json({
      message: "Your demo platform session is unavailable.",
    });
  }

  request.employee = employee;
  next();
}

platformRouter.post("/auth/login", async (request, response) => {
  const { email, password } = request.body ?? {};

  if (
    typeof email !== "string" ||
    !email.trim() ||
    email.length > 254 ||
    typeof password !== "string" ||
    !password ||
    password.length > 128
  ) {
    return response.status(400).json({
      message: "Provide a valid email and password.",
    });
  }

  const employee = await Employee.findOne({
    email: email.trim().toLowerCase(),
  }).select("+passwordHash");

  const passwordMatches =
    employee &&
    (await verifyPassword(password, employee.passwordHash));

  if (!employee?.active || !passwordMatches) {
    return response.status(401).json({
      message: "Invalid credentials or account unavailable.",
    });
  }

  await new Promise((resolve, reject) => {
    request.session.regenerate((error) => {
      if (error) reject(error);
      else resolve();
    });
  });

  request.session.employeeId = employee._id.toString();

  await new Promise((resolve, reject) => {
    request.session.save((error) => {
      if (error) reject(error);
      else resolve();
    });
  });

  return response.json({
    employee: publicEmployee(employee),
  });
});

platformRouter.get(
  "/auth/me",
  requireEmployeeSession,
  (request, response) => {
    response.json({
      employee: publicEmployee(request.employee),
    });
  },
);

platformRouter.post("/auth/logout", (request, response, next) => {
  request.session.destroy((error) => {
    if (error) return next(error);

    response.clearCookie("hris_demo.sid", { path: "/" });
    response.sendStatus(204);
  });
});

function getExcomConfiguration() {
  const backend = new URL(process.env.EXCOM_BACKEND_URL);
  const frontend = new URL(process.env.EXCOM_FRONTEND_URL);

  for (const url of [backend, frontend]) {
    const localHttp =
      url.protocol === "http:" &&
      ["localhost", "127.0.0.1", "[::1]"].includes(url.hostname);

    if (
      (url.protocol !== "https:" && !localHttp) ||
      url.username ||
      url.password ||
      url.search ||
      url.hash ||
      url.pathname !== "/"
    ) {
      throw new Error("Invalid EXCOM integration URL configuration.");
    }
  }

  const clientId = process.env.EXCOM_INTEGRATION_CLIENT_ID;
  const key = process.env.EXCOM_INTEGRATION_KEY;

  if (
    !clientId ||
    !/^[A-Za-z0-9_-]{1,100}$/.test(clientId) ||
    !key ||
    !/^[A-Fa-f0-9]{64,256}$/.test(key)
  ) {
    throw new Error("Missing or invalid EXCOM integration credentials.");
  }

  return { backend, frontend, clientId, key };
}

platformRouter.get(
  "/excom/open",
  requireEmployeeSession,
  (_request, response) => {
    const { frontend } = getExcomConfiguration();

    response.set("Referrer-Policy", "no-referrer");

    return response.redirect(
      303,
      new URL("/api/integrations/sso/start", frontend).href,
    );
  },
);

platformRouter.get(
  "/excom/continue",
  requireEmployeeSession,
  async (request, response) => {
    response.set("Referrer-Policy", "no-referrer");

    const requestId = request.query.requestId;

    if (
      typeof requestId !== "string" ||
      !/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i.test(
        requestId,
      )
    ) {
      return response.status(400).json({
        message: "Invalid EXCOM login request.",
      });
    }

    const { backend, frontend, clientId, key } =
      getExcomConfiguration();

    const employee = request.employee;

    let upstream;

    try {
      upstream = await fetch(
        new URL("/api/integrations/sso/launch", backend),
        {
          method: "POST",
          redirect: "error",
          signal: AbortSignal.timeout(10000),
          headers: {
            Accept: "application/json",
            "Content-Type": "application/json",
            Authorization: `Bearer ${key}`,
            "X-Integration-Client": clientId,
          },
          body: JSON.stringify({
            requestId,
            employeeId: employee.employeeId,
            firstName: employee.firstName,
            lastName: employee.lastName,
            email: employee.email,
            designation: employee.designation,
          }),
        },
      );
    } catch {
      return response.status(502).json({
        message: "EXCOM is unavailable. Return to the demo and try again.",
      });
    }

    if (!upstream.ok) {
      const accountUnavailable = [403, 409].includes(upstream.status);

      return response.status(accountUnavailable ? upstream.status : 502).json({
        message: accountUnavailable
          ? "EXCOM could not link or authorize this employee. Contact an administrator."
          : "EXCOM login could not be started. Check the integration configuration.",
      });
    }

    try {
      const result = await upstream.json();

      if (typeof result.launchUrl !== "string") {
        throw new Error("Missing launch URL.");
      }

      const launchUrl = new URL(result.launchUrl);
      const fragment = new URLSearchParams(launchUrl.hash.slice(1));

      if (
        launchUrl.origin !== frontend.origin ||
        launchUrl.pathname !== "/integration/complete" ||
        launchUrl.username ||
        launchUrl.password ||
        launchUrl.search ||
        fragment.get("requestId") !== requestId ||
        !/^[A-Za-z0-9_-]{43}$/.test(fragment.get("code") ?? "")
      ) {
        throw new Error("Invalid launch URL.");
      }

      return response.redirect(303, launchUrl.href);
    } catch {
      return response.status(502).json({
        message: "EXCOM returned an invalid login response.",
      });
    }
  },
);