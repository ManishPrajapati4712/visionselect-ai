import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { ArrowRight, ClipboardList, LineChart, Users } from "lucide-react";
import { Logo } from "@/components/brand/Logo";
import { GlassCard } from "@/components/common/GlassCard";
import { useApp } from "@/context/AppContext";
import { roleCopy } from "@/services/playerService";
import type { Role } from "@/types";

export const Route = createFileRoute("/roles")({
  head: () => ({
    meta: [
      { title: "Choose your role — VisionSelect AI" },
      {
        name: "description",
        content:
          "Coach, Selector or Player — VisionSelect AI tailors the evidence, rankings and training tools to how you work.",
      },
      { property: "og:title", content: "Choose your role — VisionSelect AI" },
      {
        property: "og:description",
        content: "Three tailored paths into explainable cricket talent evaluation.",
      },
    ],
  }),
  component: RoleSelection,
});

const icons: Record<Role, typeof Users> = {
  coach: ClipboardList,
  selector: Users,
  player: LineChart,
};

function RoleSelection() {
  const { setRole } = useApp();
  const navigate = useNavigate();

  const choose = (r: Role) => {
    setRole(r);
    void navigate({ to: "/dashboard" });
  };

  return (
    <main className="hero-bg min-h-screen px-5 py-14">
      <div className="mx-auto max-w-6xl">
        <Logo />
        <div className="animate-fade-up mt-14 max-w-2xl">
          <p className="label-tech">Step 1 of 2</p>
          <h1 className="telemetry mt-3 text-4xl text-foreground sm:text-5xl">
            How will you use VisionSelect?
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
            The evidence stays the same for everyone — the emphasis changes. Pick the workspace that
            matches your decisions.
          </p>
        </div>

        <div className="mt-12 grid gap-5 md:grid-cols-3">
          {(Object.keys(roleCopy) as Role[]).map((r, i) => {
            const Icon = icons[r];
            const copy = roleCopy[r];
            return (
              <GlassCard
                key={r}
                tilt
                className="lift animate-fade-up cursor-pointer p-7"
                style={{ animationDelay: `${i * 80}ms` }}
                onClick={() => choose(r)}
              >
                <div className="grid h-11 w-11 place-items-center rounded-xl bg-primary/15 ring-1 ring-primary/30">
                  <Icon className="h-5 w-5 text-cyan" />
                </div>
                <h2 className="telemetry mt-6 text-2xl text-foreground">{copy.title}</h2>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{copy.blurb}</p>
                <ul className="mt-6 space-y-2 border-t border-border pt-5 text-sm text-muted-foreground">
                  {copy.focus.map((f) => (
                    <li key={f} className="flex items-center gap-2">
                      <span className="h-1 w-1 rounded-full bg-cyan" />
                      {f}
                    </li>
                  ))}
                </ul>
                <span className="mt-7 inline-flex items-center gap-2 text-sm text-cyan">
                  Continue as {copy.title} <ArrowRight className="h-4 w-4" />
                </span>
              </GlassCard>
            );
          })}
        </div>
      </div>
    </main>
  );
}