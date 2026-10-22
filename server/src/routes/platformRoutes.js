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