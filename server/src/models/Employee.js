import mongoose from "mongoose";

const employeeSchema = new mongoose.Schema(
  {
    employeeId: {
      type: String,
      required: true,
      unique: true,
      immutable: true,
      trim: true,
      maxlength: 100,
    },

    firstName: {
      type: String,
      required: true,
      trim: true,
      maxlength: 255,
    },

    lastName: {
      type: String,
      required: true,
      trim: true,
      maxlength: 255,
    },

    email: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      lowercase: true,
      maxlength: 254,
    },

    designation: {
      type: String,
      trim: true,
      maxlength: 255,
      default: "",
    },

    active: {
      type: Boolean,
      default: true,
    },

    passwordHash: {
      type: String,
      required: true,
      select: false,
    },
  },
  {
    timestamps: true,
  },
);

export const Employee = mongoose.model(
  "Employee",
  employeeSchema,
);