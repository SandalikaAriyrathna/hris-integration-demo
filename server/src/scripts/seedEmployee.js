import mongoose from "mongoose";

import { Employee } from "../models/Employee.js";
import { hashPassword } from "../lib/password.js";

async function seed() {
  const mongoUri = process.env.MONGODB_URI;
  const databaseName = process.env.MONGODB_DB;

  if (!mongoUri || databaseName !== "hris_integration_demo") {
    throw new Error("Configure the dedicated demo database.");
  }

  try {
    await mongoose.connect(mongoUri, {
      dbName: databaseName,
      serverSelectionTimeoutMS: 10000,
    });

    await Employee.init();

    const existing = await Employee.findOne({
      employeeId: "MERN-DEMO-001",
    });

    if (existing) {
      console.log("Demo employee already exists. No changes made.");
      return;
    }

    const passwordHash = await hashPassword(
      process.env.DEMO_EMPLOYEE_PASSWORD,
    );

    await Employee.create({
      employeeId: "MERN-DEMO-001",
      firstName: "Asha",
      lastName: "Silva",
      email: "asha.demo@example.com",
      designation: "Quantity Surveyor",
      active: true,
      passwordHash,
    });

    console.log("Demo employee created: asha.demo@example.com");
  } finally {
    await mongoose.disconnect();
  }
}

seed().catch((error) => {
  console.error(
    `Employee setup failed (${error.name}). Check the demo configuration.`,
  );
  process.exitCode = 1;
});