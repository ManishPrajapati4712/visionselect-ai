import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight, MessageSquare, Scale, TrendingUp } from "lucide-react";
import {
  Area,
  AreaChart,
  PolarAngleAxis,
  PolarGrid,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { ScoreRing } from "@/components/common/ScoreRing";
import { AgreeDisagreeReview } from "@/components/common/AgreeDisagreeReview";
import { MetricBar } from "@/components/common/MetricBar";
import { useApp } from "@/context/AppContext";
import { getPlayer } from "@/data/players";

export const Route = createFileRoute("/results")({
  head: () => ({
    meta: [
      { title: "Player results — VisionSelect AI" },
      {
        name: "description",
        content:
          "Overall talent score, metric breakdown, trend history and Gemini's plain-language read on the player.",
      },
      { property: "og:title", content: "Player results — VisionSelect AI" },
      {
        property: "og:description",
        content: "A talent score you can interrogate, metric by metric.",
      },
    ],
  }),
  component: Results,
});

function Results() {
  const { activePlayerId } = useApp();
  const player = getPlayer(activePlayerId);

  const radarData = [
    { k: "Batting", v: player.metrics.batting },
    { k: "Bowling", v: player.metrics.bowling },
    { k: "Fielding", v: player.metrics.fielding },
    { k: "Fitness", v: player.metrics.fitness },
    { k: "Consistency", v: player.metrics.consistency },
  ];

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Step 3 · Evaluation"
        title={player.name}
        description={`${player.role} · ${player.team} · ${player.region} · ${player.matches} matches analysed · last updated ${player.lastAnalyzed}`}
        actions={
          <>
            <Link to="/explain">
              <MagneticButton variant="outline">Why this score?</MagneticButton>
            </Link>
            <Link to="/report">
              <MagneticButton>Coach report</MagneticButton>
            </Link>
          </>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
        <GlassCard tilt glow="primary" className="p-7">
          <div className="grid place-items-center">
            <ScoreRing value={player.overallScore} sublabel={`${player.confidence}% confidence`} />
          </div>
          <div className="mt-7 grid grid-cols-2 gap-3">
            {[
              { k: player.age, v: "Age" },
              { k: player.matches, v: "Matches" },
              { k: player.battingStyle, v: "Batting" },
              { k: player.bowlingStyle, v: "Bowling" },
            ].map((s) => (
              <div key={s.v} className="rounded-lg border border-border bg-surface/50 p-3">
                <p className="text-sm text-foreground">{s.k}</p>
                <p className="mt-0.5 text-[11px] text-muted-foreground">{s.v}</p>
              </div>
            ))}
          </div>
          <Link
            to="/assistant"
            className="focus-ring mt-5 flex items-center justify-between rounded-xl border border-border bg-surface/50 p-4 text-sm"
          >
            <span className="flex items-center gap-2 text-foreground">
              <MessageSquare className="h-4 w-4 text-cyan" /> Question this evaluation
            </span>
            <ArrowRight className="h-4 w-4 text-cyan" />
          </Link>
        </GlassCard>

        <div className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-2">
            <GlassCard className="p-6">
              <h2 className="telemetry text-lg text-foreground">Metric breakdown</h2>
              <div className="mt-5 space-y-4">
                <MetricBar label="Batting technique" value={player.metrics.batting} />
                <MetricBar label="Bowling" value={player.metrics.bowling} tone="cyan" />
                <MetricBar label="Fielding" value={player.metrics.fielding} tone="success" />
                <MetricBar label="Fitness" value={player.metrics.fitness} tone="warning" />
                <MetricBar label="Consistency" value={player.metrics.consistency} tone="cyan" />
              </div>
            </GlassCard>

            <GlassCard className="p-6">
              <h2 className="telemetry text-lg text-foreground">Skill profile</h2>
              <div className="mt-2 h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <RadarChart data={radarData} outerRadius="72%">
                    <PolarGrid stroke="var(--border)" />
                    <PolarAngleAxis
                      dataKey="k"
                      tick={{ fill: "var(--muted-foreground)", fontSize: 11 }}
                    />
                    <Radar
                      dataKey="v"
                      stroke="var(--cyan)"
                      fill="var(--cyan)"
                      fillOpacity={0.22}
                    />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
            </GlassCard>
          </div>

          <GlassCard className="p-6">
            <div className="flex items-center justify-between">
              <h2 className="telemetry text-lg text-foreground">Score trend</h2>
              <span className="flex items-center gap-1.5 text-xs text-success">
                <TrendingUp className="h-3.5 w-3.5" /> improving
              </span>
            </div>
            <div className="mt-4 h-56">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={player.trend} margin={{ left: -22, right: 6, top: 8 }}>
                  <defs>
                    <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="var(--primary)" stopOpacity={0.45} />
                      <stop offset="100%" stopColor="var(--primary)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis
                    dataKey="label"
                    tick={{ fill: "var(--muted-foreground)", fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    domain={[50, 100]}
                    tick={{ fill: "var(--muted-foreground)", fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip
                    contentStyle={{
                      background: "var(--surface)",
                      border: "1px solid var(--border)",
                      borderRadius: 12,
                      color: "var(--foreground)",
                      fontSize: 12,
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="score"
                    stroke="var(--primary)"
                    strokeWidth={2}
                    fill="url(#trendFill)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </GlassCard>

          <div className="grid gap-6 lg:grid-cols-2">
            <GlassCard className="p-6">
              <p className="label-tech text-success">Strengths</p>
              <ul className="mt-4 space-y-3 text-sm text-muted-foreground">
                {player.strengths.map((s) => (
                  <li key={s} className="flex gap-2.5">
                    <span className="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-success" />
                    {s}
                  </li>
                ))}
              </ul>
            </GlassCard>
            <GlassCard className="p-6">
              <p className="label-tech text-warning">Development areas</p>
              <ul className="mt-4 space-y-3 text-sm text-muted-foreground">
                {player.weaknesses.map((s) => (
                  <li key={s} className="flex gap-2.5">
                    <span className="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-warning" />
                    {s}
                  </li>
                ))}
              </ul>
            </GlassCard>
          </div>

          <AgreeDisagreeReview context="this AI summary" />

          <Link to="/fairness" className="focus-ring block rounded-2xl">
            <GlassCard className="lift flex items-center gap-4 p-5">
              <Scale className="h-5 w-5 text-cyan" />
              <p className="flex-1 text-sm text-muted-foreground">
                This evaluation passed all four bias controls — see the audit trail.
              </p>
              <ArrowRight className="h-4 w-4 text-cyan" />
            </GlassCard>
          </Link>
        </div>
      </div>
    </AppLayout>
  );
}