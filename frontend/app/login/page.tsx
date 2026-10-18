"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Button from "@/components/Button";
import TextField from "@/components/TextField";
import { ApiError, errorMessage, login } from "@/lib/api";
import { getSession, saveSession } from "@/lib/session";
import { validateLogin, type LoginErrors } from "@/lib/validation";
import styles from "./login.module.css";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<LoginErrors>({});
  const [formError, setFormError] = useState("");
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (getSession()) router.replace("/employees");
  }, [router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validateLogin(email, password);
    setErrors(found);
    setFormError("");
    if (Object.keys(found).length > 0) return;

    setSending(true);
    try {
      saveSession(await login(email.trim(), password));
      router.replace("/employees");
    } catch (error) {
      const invalid = error instanceof ApiError && error.status === 401;
      setFormError(invalid ? "Invalid email or password." : errorMessage(error));
      setSending(false);
    }
  }

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit} noValidate aria-labelledby="login-title">
        <div>
          <h1 id="login-title">Pesira Employee Records</h1>
          <p className={styles.subtitle}>Sign in to continue</p>
        </div>
        {formError && (
          <p className={styles.formError} role="alert">
            {formError}
          </p>
        )}
        <TextField
          id="email"
          label="Email"
          type="email"
          autoComplete="username"
          value={email}
          error={errors.email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <TextField
          id="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          value={password}
          error={errors.password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <Button type="submit" variant="primary" disabled={sending}>
          {sending ? "Signing in…" : "Sign in"}
        </Button>
      </form>
    </main>
  );
}
