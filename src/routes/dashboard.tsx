import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { ArrowUpRight, Filter, Scale, Search, Sparkle, Upload } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { useApp } from "@/context/AppContext";
import { searchPlayers } from "@/services/playerService";
import { activityFeed, recentAnalyses } from "@/data/analysis";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/dashboard")({
  head: () => ({
    meta: [
      { title: "Dashboard — VisionSelect AI" },
      {
        name: "description",
        content:
          "Recent video analyses, player library, cohort rankings and live AI activity in one command centre.",
      },
      { property: "og:title", content: "Dashboard — VisionSelect AI" },
      {
        property: "og:description",
        content: "Your cricket talent command centre: analyses, rankings and AI activity.",
      },
    ],
  }),
  component: Dashboard,
});

const roleStats = {
  coach: [
    { k: "6", v: "Players in programme" },
    { k: "4", v: "Active training plans" },
    { k: "+5.2", v: "Avg score delta / 6wk" },
    { k: "2", v: "Sessions awaiting review" },
  ],
  selector: [
    { k: "24", v: "Candidates ranked" },
    { k: "96", v: "Fairness score" },
    { k: "±1.8", v: "Regional variance (pts)" },
    { k: "11", v: "Human overrides accepted" },
  ],
  player: [
    { k: "87", v: "Your current AI score" },
    { k: "+16", v: "Improvement since March" },
    { k: "24", v: "Matches analysed" },
    { k: "2", v: "Priority drills this week" },
  ],
} as const;

const roleShortcuts = {
  coach: [
    { to: "/report", label: "Open training plan", icon: Sparkle },
    { to: "/explain", label: "Weakness drill-down", icon: ArrowUpRight },
  ],
  selector: [
    { to: "/compare", label: "Compare shortlist", icon: ArrowUpRight },
    { to: "/fairness", label: "Fairness audit", icon: Scale },
  ],
  player: [
    { to: "/results", label: "My latest results", icon: ArrowUpRight },
    { to: "/explain", label: "Why this score?", icon: Sparkle },
  ],
} as const;

function Dashboard() {
  const { role } = useApp();
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("all");
  const results = useMemo(() => searchPlayers(query, filter), [query, filter]);

  return (
    <AppLayout>
      <PageHeader
        eyebrow={`${role} workspace`}
        title={
          role === "coach"
            ? "Development command centre"
            : role === "selector"
              ? "Selection intelligence"
              : "Your performance intelligence"
        }
        description={
          role === "coach"
            ? "Every analysis links straight to the drill that moves the number."
            : role === "selector"
              ? "Rank on measured evidence, and carry the fairness audit into the room with you."
              : "See what the model saw, and exactly what will move your score next."
        }
        actions={
          <>
            {roleShortcuts[role].map((s) => (
              <Link key={s.to} to={s.to}>
                <MagneticButton variant="outline">
                  <s.icon className="h-4 w-4" /> {s.label}
                </MagneticButton>
              </Link>
            ))}
            <Link to="/upload">
              <MagneticButton>
                <Upload className="h-4 w-4" /> New analysis
              </MagneticButton>
            </Link>
          </>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {roleStats[role].map((s, i) => (
          <GlassCard key={s.v} tilt className="animate-fade-up p-5" style={{ animationDelay: `${i * 60}ms` }}>
            <p className="telemetry text-3xl text-foreground">{s.k}</p>
            <p className="mt-1.5 text-xs text-muted-foreground">{s.v}</p>
          </GlassCard>
        ))}
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.6fr_1fr]">
        <div className="space-y-6">
          <GlassCard className="p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="telemetry text-lg text-foreground">Recent analyses</h2>
              <span className="label-tech">Last 7 days</span>
            </div>
            <div className="mt-5 overflow-x-auto">
              <table className="w-full min-w-[560px] text-sm">
                <thead>
                  <tr className="border-b border-border text-left">
                    {["Player", "Venue", "Duration", "Score", "Confidence", "Status"].map((h) => (
                      <th key={h} className="label-tech pb-3 font-semibold">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {recentAnalyses.map((a) => (
                    <tr key={a.id} className="border-b border-border/60 last:border-0">
                      <td className="py-3.5">
                        <Link to="/results" className="focus-ring text-foreground hover:text-cyan">
                          {a.playerName}
                        </Link>
                        <span className="block text-xs text-muted-foreground">{a.date}</span>
                      </td>
                      <td className="text-muted-foreground">{a.venue}</td>
                      <td className="telemetry text-muted-foreground">{a.duration}</td>
                      <td className="telemetry text-foreground">{a.score}</td>
                      <td className="telemetry text-cyan">{a.confidence}%</td>
                      <td>
                        <span
                          className={cn(
                            "rounded-full px-2.5 py-1 text-[11px] capitalize",
                            a.status === "complete" && "bg-success/15 text-success",
                            a.status === "review" && "bg-warning/15 text-warning",
                            a.status === "processing" && "bg-primary/15 text-primary",
                          )}
                        >
                          {a.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="telemetry text-lg text-foreground">Player library</h2>
              <div className="flex items-center gap-2">
                <label className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <input
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search players or teams"
                    aria-label="Search players"
                    className="focus-ring w-52 rounded-lg border border-border bg-surface/60 py-2 pl-9 pr-3 text-sm text-foreground placeholder:text-muted-foreground"
                  />
                </label>
                <label className="relative">
                  <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <select
                    value={filter}
                    onChange={(e) => setFilter(e.target.value)}
                    aria-label="Filter by discipline"
                    className="focus-ring appearance-none rounded-lg border border-border bg-surface/60 py-2 pl-9 pr-4 text-sm text-foreground"
                  >
                    <option value="all">All roles</option>
                    <option value="batter">Batters</option>
                    <option value="bowler">Bowlers</option>
                    <option value="all-rounder">All-rounders</option>
                  </select>
                </label>
              </div>
            </div>

            {results.length === 0 ? (
              <div className="mt-8 rounded-xl border border-dashed border-border py-14 text-center">
                <p className="telemetry text-foreground">No players match that filter</p>
                <p className="mt-2 text-sm text-muted-foreground">
                  Try a different discipline, or upload footage to add a new player.
                </p>
              </div>
            ) : (
              <div className="mt-5 grid gap-3 sm:grid-cols-2">
                {results.map((p) => (
                  <Link key={p.id} to="/results" className="focus-ring rounded-xl">
                    <div className="lift flex items-center gap-4 rounded-xl border border-border bg-surface/50 p-4">
                      <span className="telemetry grid h-11 w-11 place-items-center rounded-lg bg-primary/15 text-sm text-cyan">
                        {p.initials}
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-foreground">{p.name}</p>
                        <p className="truncate text-xs text-muted-foreground">
                          {p.role} · {p.team}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="telemetry text-xl text-foreground">{p.overallScore}</p>
                        <p className="text-[11px] text-muted-foreground">{p.confidence}% conf.</p>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </GlassCard>
        </div>

        <GlassCard className="h-fit p-6">
          <h2 className="telemetry text-lg text-foreground">Activity</h2>
          <ul className="mt-5 space-y-4">
            {activityFeed.map((a) => (
              <li key={a.id} className="flex gap-3">
                <span
                  className={cn(
                    "mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full",
                    a.tone === "success" && "bg-success",
                    a.tone === "warning" && "bg-warning",
                    a.tone === "info" && "bg-cyan",
                  )}
                />
                <div>
                  <p className="text-sm leading-relaxed text-foreground/85">{a.text}</p>
                  <p className="mt-1 text-[11px] text-muted-foreground">{a.time}</p>
                </div>
              </li>
            ))}
          </ul>
          <Link
            to="/assistant"
            className="focus-ring mt-6 flex items-center justify-between rounded-xl border border-border bg-surface/50 p-4 text-sm"
          >
            <span className="text-foreground">Ask Gemini about any report</span>
            <ArrowUpRight className="h-4 w-4 text-cyan" />
          </Link>
        </GlassCard>
      </div>
    </AppLayout>
  );
}