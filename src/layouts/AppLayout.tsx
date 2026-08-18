import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import type { ReactNode } from "react";
import {
  Activity,
  BarChart3,
  Bell,
  Braces,
  FileText,
  Gauge,
  LogOut,
  MessageSquare,
  ScanLine,
  Scale as ScaleIcon,
  Settings,
  UserRound,
  Users,
} from "lucide-react";
import { Logo } from "@/components/brand/Logo";
import { useApp } from "@/context/AppContext";
import { useAuth } from "@/context/AuthContext";
import { useSpotlight } from "@/hooks/useSpotlight";
import { cn } from "@/lib/utils";

const nav = [
  { to: "/dashboard", label: "Dashboard", icon: Gauge },
  { to: "/upload", label: "Upload Video", icon: ScanLine },
  { to: "/analysis", label: "AI Analysis", icon: Activity },
  { to: "/results", label: "Results", icon: BarChart3 },
  { to: "/explain", label: "Why This Score?", icon: Braces },
  { to: "/assistant", label: "Gemini Assistant", icon: MessageSquare },
  { to: "/fairness", label: "Fairness Report", icon: ScaleIcon },
  { to: "/compare", label: "Compare Players", icon: Users },
  { to: "/report", label: "Coach Report", icon: FileText },
] as const;

export function AppLayout({ children }: { children: ReactNode }) {
  const spotlight = useSpotlight<HTMLDivElement>();
  const { role } = useApp();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  const handleLogout = async () => {
    await logout();
    void navigate({ to: "/login" });
  };

  return (
    <div className="min-h-screen bg-background">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-border bg-surface/70 backdrop-blur-xl lg:flex">
        <div className="flex h-16 items-center border-b border-border px-5">
          <Link to="/" className="focus-ring rounded-lg">
            <Logo />
          </Link>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {nav.map(({ to, label, icon: Icon }) => {
            const active = pathname === to;
            return (
              <Link
                key={to}
                to={to}
                className={cn(
                  "focus-ring flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors",
                  active
                    ? "bg-surface-2 text-foreground ring-1 ring-primary/30"
                    : "text-muted-foreground hover:bg-surface-2/60 hover:text-foreground",
                )}
              >
                <Icon className={cn("h-4 w-4", active && "text-cyan")} />
                {label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-border p-3">
          <Link
            to="/settings"
            className="focus-ring flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            <Settings className="h-4 w-4" />
            Settings
          </Link>
          {user ? (
            <>
              <Link
                to="/profile"
                className="focus-ring mt-1 flex items-center gap-3 rounded-lg bg-surface-2/60 px-3 py-2.5 text-sm text-foreground"
              >
                <span className="telemetry grid h-7 w-7 place-items-center rounded-md bg-primary/20 text-xs text-cyan">
                  {user.displayName.slice(0, 2).toUpperCase()}
                </span>
                <span className="flex-1 truncate">
                  {user.displayName}
                  <span className="block text-[11px] capitalize text-muted-foreground">
                    {user.role.toLowerCase()}
                  </span>
                </span>
              </Link>
              <button
                onClick={() => void handleLogout()}
                className="focus-ring mt-1 flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-muted-foreground transition-colors hover:bg-surface-2/60 hover:text-foreground"
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </button>
            </>
          ) : (
            <Link
              to="/login"
              className="focus-ring mt-1 flex items-center gap-3 rounded-lg bg-surface-2/60 px-3 py-2.5 text-sm text-foreground"
            >
              <span className="telemetry grid h-7 w-7 place-items-center rounded-md bg-primary/20 text-xs text-cyan">
                ??
              </span>
              <span className="flex-1 truncate">
                Sign in
                <span className="block text-[11px] text-muted-foreground">Not signed in</span>
              </span>
            </Link>
          )}
        </div>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur-xl sm:px-6">
          <Link to="/" className="focus-ring rounded-lg lg:hidden">
            <Logo compact />
          </Link>
          <div className="ml-auto flex items-center gap-2">
            <span className="hidden items-center gap-2 rounded-full border border-border bg-surface/60 px-3 py-1.5 text-xs text-muted-foreground sm:inline-flex">
              <span className="h-1.5 w-1.5 rounded-full bg-success" />
              Gemini analyst online
            </span>
            <Link
              to="/notifications"
              aria-label="Notifications"
              className="focus-ring relative grid h-9 w-9 place-items-center rounded-lg border border-border bg-surface/60 text-muted-foreground transition-colors hover:text-foreground"
            >
              <Bell className="h-4 w-4" />
              <span className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-cyan" />
            </Link>
            <Link
              to="/roles"
              className="focus-ring hidden items-center gap-2 rounded-lg border border-border bg-surface/60 px-3 py-2 text-xs text-muted-foreground transition-colors hover:text-foreground sm:inline-flex"
            >
              <UserRound className="h-3.5 w-3.5" />
              Switch role
            </Link>
          </div>
        </header>

        <div
          ref={spotlight}
          className="relative min-h-[calc(100vh-4rem)] px-4 py-8 sm:px-6 lg:px-10"
        >
          <span
            aria-hidden
            className="pointer-events-none absolute inset-0 opacity-70"
            style={{
              background:
                "radial-gradient(600px circle at var(--mx, 30%) var(--my, 0%), oklch(1 0 0 / 3.5%), transparent 65%)",
            }}
          />
          <div className="relative mx-auto max-w-7xl">{children}</div>
        </div>

        <nav className="sticky bottom-0 z-20 flex items-center gap-1 overflow-x-auto border-t border-border bg-background/90 px-2 py-2 backdrop-blur-xl lg:hidden">
          {nav.map(({ to, label, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              className={cn(
                "focus-ring flex min-w-16 flex-col items-center gap-1 rounded-lg px-2 py-1.5 text-[10px]",
                pathname === to ? "text-cyan" : "text-muted-foreground",
              )}
            >
              <Icon className="h-4 w-4" />
              {label.split(" ")[0]}
            </Link>
          ))}
        </nav>
      </div>
    </div>
  );
}