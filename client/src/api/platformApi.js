export class PlatformApiError extends Error {
  constructor(status, message) {
    super(message);
    this.name = "PlatformApiError";
    this.status = status;
  }
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  if (typeof options.body === "string") {
    headers.set("Content-Type", "application/json");
  }

  const method = (options.method ?? "GET").toUpperCase();

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrf = await request("/api/auth/csrf");

    if (
      csrf?.headerName !== "X-CSRF-TOKEN" ||
      typeof csrf.token !== "string" ||
      !csrf.token
    ) {
      throw new PlatformApiError(
        500,
        "Unable to obtain a security token. Please refresh and try again.",
      );
    }

    headers.set(csrf.headerName, csrf.token);
  }

  const response = await fetch(path, {
    ...options,
    headers,
    credentials: "same-origin",
    redirect: "error",
  });

  if (!response.ok) {
    let message = "Unable to complete the request.";

    try {
      const body = await response.json();

      if (typeof body.message === "string") {
        message = body.message;
      }
    } catch {
      // Some errors may not contain JSON.
    }

    throw new PlatformApiError(response.status, message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export async function loginToPlatform(email, password) {
  const result = await request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  return result.employee;
}

export async function getCurrentEmployee() {
  try {
    const result = await request("/api/auth/me");
    return result.employee;
  } catch (error) {
    if (error instanceof PlatformApiError && error.status === 401) {
      return null;
    }

    throw error;
  }
}

export async function logoutFromPlatform() {
  await request("/api/auth/logout", {
    method: "POST",
  });
}