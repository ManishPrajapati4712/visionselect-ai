import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Eye, EyeOff, UserPlus } from "lucide-react";
import { Logo } from "@/components/brand/Logo";
import { GlassCard } from "@/components/common/GlassCard";
import { useAuth } from "@/context/AuthContext";

export const Route = createFileRoute("/register")({
  head: () => ({
    meta: [
      { title: "Create account — VisionSelect AI" },
      {
        name: "description",
        content: "Create a VisionSelect AI account to start analysing cricket footage.",
      },
    ],
  }),
  component: RegisterPage,
});

// Backend-accepted roles (security-contract.md — users cannot self-assign ADMIN)
const REGISTERABLE_ROLES = [
  { value: "COACH", label: "Coach" },
  { value: "SELECTOR", label: "Selector" },
  { value: "PLAYER", label: "Player" },
] as const;

function RegisterPage() {
  const { register, isAuthenticating, authError, clearAuthError } = useAuth();
  const navigate = useNavigate();

  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<string>("COACH");
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{
    displayName?: string;
    email?: string;
    password?: string;
    role?: string;
  }>({});

  const validate = () => {
    const errors: typeof fieldErrors = {};
    if (!displayName.trim()) errors.displayName = "Display name is required.";
    if (!email.trim()) errors.email = "Email is required.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      errors.email = "Enter a valid email address.";
    if (!password) errors.password = "Password is required.";
    else if (password.length < 8) errors.password = "Password must be at least 8 characters.";
    if (!role) errors.role = "Please select a role.";
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearAuthError();
    if (!validate()) return;

    try {
      await register({
        displayName: displayName.trim(),
        email: email.trim(),
        password,
        role,
      });
      void navigate({ to: "/dashboard" });
    } catch {
      // authError is set by AuthContext
    }
  };

  return (
    <main className="hero-bg flex min-h-screen items-center justify-center px-5 py-14">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Logo />
          <h1 className="telemetry mt-6 text-3xl text-foreground">Create your account</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Join VisionSelect AI and start analysing cricket footage.
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

            {/* Display name */}
            <div className="mb-4">
              <label htmlFor="reg-name" className="mb-1.5 block text-sm text-foreground">
                Display name
              </label>
              <input
                id="reg-name"
                type="text"
                autoComplete="name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="Priya Verma"
                disabled={isAuthenticating}
                className={
                  "focus-ring w-full rounded-lg border bg-surface/60 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground disabled:opacity-50 " +
                  (fieldErrors.displayName ? "border-danger" : "border-border")
                }
              />
              {fieldErrors.displayName && (
                <p className="mt-1 text-xs text-danger">{fieldErrors.displayName}</p>
              )}
            </div>

            {/* Email */}
            <div className="mb-4">
              <label htmlFor="reg-email" className="mb-1.5 block text-sm text-foreground">
                Email address
              </label>
              <input
                id="reg-email"
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
            <div className="mb-4">
              <label htmlFor="reg-password" className="mb-1.5 block text-sm text-foreground">
                Password <span className="text-muted-foreground">(min. 8 characters)</span>
              </label>
              <div className="relative">
                <input
                  id="reg-password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="new-password"
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

            {/* Role */}
            <div className="mb-6">
              <label htmlFor="reg-role" className="mb-1.5 block text-sm text-foreground">
                Your role
              </label>
              <select
                id="reg-role"
                value={role}
                onChange={(e) => setRole(e.target.value)}
                disabled={isAuthenticating}
                className={
                  "focus-ring w-full rounded-lg border bg-surface/60 px-4 py-2.5 text-sm text-foreground disabled:opacity-50 " +
                  (fieldErrors.role ? "border-danger" : "border-border")
                }
              >
                {REGISTERABLE_ROLES.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </select>
              {fieldErrors.role && (
                <p className="mt-1 text-xs text-danger">{fieldErrors.role}</p>
              )}
            </div>

            <button
              id="register-submit"
              type="submit"
              disabled={isAuthenticating}
              className="focus-ring flex w-full items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-50"
            >
              {isAuthenticating ? (
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
              ) : (
                <UserPlus className="h-4 w-4" />
              )}
              {isAuthenticating ? "Creating account…" : "Create account"}
            </button>
          </form>
        </GlassCard>

        <p className="mt-5 text-center text-sm text-muted-foreground">
          Already have an account?{" "}
          <Link to="/login" className="text-cyan transition-opacity hover:opacity-80">
            Sign in
          </Link>
        </p>
      </div>
    </main>
  );
}
