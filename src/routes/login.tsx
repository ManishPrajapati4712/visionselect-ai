import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Eye, EyeOff, LogIn } from "lucide-react";
import { Logo } from "@/components/brand/Logo";
import { GlassCard } from "@/components/common/GlassCard";
import { useAuth } from "@/context/AuthContext";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Sign in — VisionSelect AI" },
      { name: "description", content: "Sign in to VisionSelect AI to analyse cricket footage and access the platform." },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const { login, isAuthenticating, authError, clearAuthError } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

  const validate = () => {
    const errors: { email?: string; password?: string } = {};
    if (!email.trim()) errors.email = "Email is required.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = "Enter a valid email address.";
    if (!password) errors.password = "Password is required.";
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearAuthError();
    if (!validate()) return;

    try {
      await login({ email: email.trim(), password });
      void navigate({ to: "/roles" });
    } catch {
      // authError is set by AuthContext; no additional handling needed here
    }
  };

  return (
    <main className="hero-bg flex min-h-screen items-center justify-center px-5 py-14">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Logo />
          <h1 className="telemetry mt-6 text-3xl text-foreground">Welcome back</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Sign in to your account to continue.
          </p>
        </div>

        <GlassCard className="p-8">
          <form onSubmit={(e) => void handleSubmit(e)} noValidate>
            {/* Auth error banner */}
            {authError && (
              <div
                role="alert"
                className="mb-5 rounded-lg border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger"
              >
                {authError}
              </div>
            )}

            {/* Email */}
            <div className="mb-4">
              <label htmlFor="login-email" className="mb-1.5 block text-sm text-foreground">
                Email address
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                disabled={isAuthenticating}
                className={
                  "focus-ring w-full rounded-lg border bg-surface/60 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground disabled:opacity-50 " +
                  (fieldErrors.email ? "border-danger" : "border-border")
                }
              />
              {fieldErrors.email && (
                <p className="mt-1 text-xs text-danger">{fieldErrors.email}</p>
              )}
            </div>

            {/* Password */}
            <div className="mb-6">
              <label htmlFor="login-password" className="mb-1.5 block text-sm text-foreground">
                Password
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  disabled={isAuthenticating}
                  className={
                    "focus-ring w-full rounded-lg border bg-surface/60 px-4 py-2.5 pr-11 text-sm text-foreground placeholder:text-muted-foreground disabled:opacity-50 " +
                    (fieldErrors.password ? "border-danger" : "border-border")
                  }
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {fieldErrors.password && (
                <p className="mt-1 text-xs text-danger">{fieldErrors.password}</p>
              )}
            </div>

            <button
              id="login-submit"
              type="submit"
              disabled={isAuthenticating}
              className="focus-ring flex w-full items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
            >
              {isAuthenticating ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
              ) : (
                <LogIn className="h-4 w-4" />
              )}
              {isAuthenticating ? "Signing in…" : "Sign in"}
            </button>
          </form>
        </GlassCard>

        <p className="mt-5 text-center text-sm text-muted-foreground">
          Don&apos;t have an account?{" "}
          <Link
            to="/register"
            className="text-cyan transition-opacity hover:opacity-80"
          >
            Create one
          </Link>
        </p>
      </div>
    </main>
  );
}
