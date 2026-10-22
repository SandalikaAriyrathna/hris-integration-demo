import {
  pbkdf2,
  randomBytes,
  timingSafeEqual,
} from "node:crypto";
import { promisify } from "node:util";

const deriveKey = promisify(pbkdf2);

const ITERATIONS = 600_000;
const KEY_LENGTH = 32;
const DIGEST = "sha256";

export async function hashPassword(password) {
  if (
    typeof password !== "string" ||
    password.length < 12 ||
    password.length > 128
  ) {
    throw new Error("Password must contain 12–128 characters.");
  }

  const salt = randomBytes(16);

  const hash = await deriveKey(
    password,
    salt,
    ITERATIONS,
    KEY_LENGTH,
    DIGEST,
  );

  return [
    "pbkdf2-sha256",
    String(ITERATIONS),
    salt.toString("hex"),
    hash.toString("hex"),
  ].join("$");
}

export async function verifyPassword(password, storedHash) {
  if (
    typeof password !== "string" ||
    password.length > 128 ||
    typeof storedHash !== "string"
  ) {
    return false;
  }

  const parts = storedHash.split("$");

  if (
    parts.length !== 4 ||
    parts[0] !== "pbkdf2-sha256" ||
    parts[1] !== String(ITERATIONS) ||
    !/^[a-f0-9]{32}$/.test(parts[2]) ||
    !/^[a-f0-9]{64}$/.test(parts[3])
  ) {
    return false;
  }

  const actual = await deriveKey(
    password,
    Buffer.from(parts[2], "hex"),
    ITERATIONS,
    KEY_LENGTH,
    DIGEST,
  );

  const expected = Buffer.from(parts[3], "hex");

  return timingSafeEqual(actual, expected);
}