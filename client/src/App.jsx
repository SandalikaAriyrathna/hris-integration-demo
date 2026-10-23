import { useEffect, useState } from "react";

import {
  getCurrentEmployee,
  loginToPlatform,
  logoutFromPlatform,
} from "./api/platformApi";

import "./App.css";

export default function App() {
  const [employee, setEmployee] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [busy, setBusy] = useState(false);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    const restoreSession = async () => {
      try {
        const currentEmployee = await getCurrentEmployee();

        if (!cancelled) {
          setEmployee(currentEmployee);
        }
      } catch {
        if (!cancelled) {
          setError("Unable to check your session. Please try signing in.");
        }
      } finally {
        if (!cancelled) {
          setCheckingSession(false);
        }
      }
    };

    void restoreSession();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleLogin = async (event) => {
    event.preventDefault();

    if (busy) return;

    setBusy(true);
    setError("");

    try {
      const account = await loginToPlatform(email.trim(), password);
      setEmployee(account);
    } catch (error) {
      setError(error.message || "Unable to sign in.");
    } finally {
      setPassword("");
      setBusy(false);
    }
  };

  const handleLogout = async () => {
    if (busy) return;

    setBusy(true);
    setError("");

    try {
      await logoutFromPlatform();
      setEmployee(null);
    } catch {
      setError("Unable to sign out. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="platform-shell">
      <header className="platform-header">
        <div>
          <p className="eyebrow">Separate integration test platform</p>
          <h1>HRIS Demo</h1>
        </div>

        {employee && (
          <button
            type="button"
            className="secondary-button"
            onClick={handleLogout}
            disabled={busy}
          >
            {busy ? "Please wait…" : "Sign out"}
          </button>
        )}
      </header>

      {error && (
        <p className="platform-message" role="alert">
          {error}
        </p>
      )}

      {checkingSession ? (
        <section className="panel" role="status">
          Checking your platform session…
        </section>
      ) : employee ? (
        <section className="panel">
          <p className="eyebrow">Signed into HRIS</p>

          <h2>
            {employee.firstName} {employee.lastName}
          </h2>

          <dl className="employee-details">
            <div>
              <dt>Employee ID</dt>
              <dd>{employee.employeeId}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{employee.email}</dd>
            </div>
            <div>
              <dt>Designation</dt>
              <dd>{employee.designation || "Not provided"}</dd>
            </div>
          </dl>

          <div className="launch-section">
            <h2>EXCOM Resume Builder</h2>

            <p>
              Open your resume using your existing HRIS session.
              No separate EXCOM password is required.
            </p>

            <button
              type="button"
              className="primary-button"
              disabled={busy}
              onClick={() => {
                window.location.assign("/api/excom/open");
              }}
            >
              Open Resume Builder
            </button>

            <p className="integration-note">
              The launch button will be enabled when the EXCOM
              integration API is connected.
            </p>
          </div>
        </section>
      ) : (
        <section className="panel login-panel">
          <h2>Sign into the demo platform</h2>
          <p>Use your fictional employee’s demo-platform credentials.</p>

          <form onSubmit={handleLogin} className="login-form">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="username"
              maxLength={254}
              disabled={busy}
              required
            />

            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              maxLength={128}
              disabled={busy}
              required
            />

            <button
              type="submit"
              className="primary-button"
              disabled={busy}
            >
              {busy ? "Signing in…" : "Sign in"}
            </button>
          </form>
        </section>
      )}
    </main>
  );
}