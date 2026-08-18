import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import {
  PolarAngleAxis,
  PolarGrid,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip,
} from "recharts";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { StreamBlock } from "@/components/common/StreamBlock";
import { useStreamedText } from "@/hooks/useStreamedText";
import { comparisonVerdict } from "@/services/geminiService";
import { players, getPlayer } from "@/data/players";
import { cn } from "@/lib/utils";
import type { MetricKey } from "@/types";

export const Route = createFileRoute("/compare")({
  head: () => ({
    meta: [
      { title: "Compare players — VisionSelect AI" },
      {
        name: "description",
        content:
          "Head-to-head talent comparison with a radar profile, metric-by-metric deltas and Gemini's reasoned recommendation with a stated confidence level.",
      },
      { property: "og:title", content: "Compare players — VisionSelect AI" },
      {
        property: "og:description",
        content: "Two prospects, one evidence-based verdict.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Compare,
});

const METRICS: { key: MetricKey; label: string }[] = [
  { key: "batting", label: "Batting" },
  { key: "bowling", label: "Bowling" },
  { key: "fielding", label: "Fielding" },
  { key: "fitness", label: "Fitness" },
  { key: "consistency", label: "Consistency" },
];

function Compare() {
  const [aId, setAId] = useState("arjun-mehra");
  const [bId, setBId] = useState("rohan-desai");
  const a = getPlayer(aId);
  const b = getPlayer(bId);
  const { text, done } = useStreamedText(comparisonVerdict, true, 16);

  const radarData = METRICS.map((m) => ({
    k: m.label,
    a: a.metrics[m.key],
    b: b.metrics[m.key],
  }));

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Head to head"
        title="Compare players"
        description="Put two prospects side by side across every measured dimension, then read Gemini's recommendation — including where it thinks the overall score is the wrong comparison to use."
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-2">
        {[
          { label: "Player A", value: aId, set: setAId, tone: "primary" as const },
          { label: "Player B", value: bId, set: setBId, tone: "cyan" as const },
        ].map((sel) => (
          <label
            key={sel.label}
            className="block rounded-xl border border-border bg-surface/50 p-4"
          >
            <span className="label-tech">{sel.label}</span>
            <select
              value={sel.value}
              onChange={(e) => sel.set(e.target.value)}
              className="focus-ring mt-2 w-full rounded-lg border border-border bg-surface-2/60 px-3 py-2.5 text-sm text-foreground outline-none"
            >
              {players.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} · {p.role}
                </option>
              ))}
            </select>
          </label>
        ))}
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_400px]">
        <div className="space-y-6">
          <GlassCard glow="primary" className="p-6">
            <div className="grid gap-6 sm:grid-cols-2">
              {[
                { p: a, tone: "text-primary" },
                { p: b, tone: "text-cyan" },
              ].map(({ p, tone }) => (
                <div
                  key={p.id}
                  className="rounded-xl border border-border bg-surface/40 p-5 text-center"
                >
                  <span className="telemetry mx-auto grid h-12 w-12 place-items-center rounded-xl bg-surface-2 text-sm text-foreground">
                    {p.initials}
                  </span>
                  <p className="mt-3 text-sm text-foreground">{p.name}</p>
                  <p className="text-[11px] text-muted-foreground">
                    {p.role} · {p.team}
                  </p>
                  <p className={cn("telemetry mt-3 text-4xl", tone)}>{p.overallScore}</p>
                  <p className="text-[11px] text-muted-foreground">
                    {p.confidence}% confidence · {p.matches} matches
                  </p>
                </div>
              ))}
            </div>

            <div className="mt-6 h-[320px]">
              <ResponsiveContainer width="100%" height="100%">
                <RadarChart data={radarData} outerRadius="72%">
                  <PolarGrid stroke="var(--surface-2)" />
                  <PolarAngleAxis
                    dataKey="k"
                    tick={{ fill: "var(--muted-foreground)", fontSize: 11 }}
                  />
                  <Tooltip
                    contentStyle={{
                      background: "var(--surface)",
                      border: "1px solid var(--border)",
                      borderRadius: 12,
                      fontSize: 12,
                    }}
                  />
                  <Radar
                    name={a.name}
                    dataKey="a"
                    stroke="var(--primary)"
                    fill="var(--primary)"
                    fillOpacity={0.25}
                  />
                  <Radar
                    name={b.name}
                    dataKey="b"
                    stroke="var(--cyan)"
                    fill="var(--cyan)"
                    fillOpacity={0.18}
                  />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <h2 className="telemetry text-lg text-foreground">Metric by metric</h2>
            <div className="mt-5 space-y-4">
              {METRICS.map((m) => {
                const va = a.metrics[m.key];
                const vb = b.metrics[m.key];
                return (
                  <div key={m.key}>
                    <div className="flex items-center justify-between text-xs">
                      <span className={cn("telemetry", va >= vb ? "text-primary" : "text-muted-foreground")}>
                        {va}
                      </span>
                      <span className="text-muted-foreground">{m.label}</span>
                      <span className={cn("telemetry", vb >= va ? "text-cyan" : "text-muted-foreground")}>
                        {vb}
                      </span>
                    </div>
                    <div className="mt-2 flex items-center gap-1">
                      <div className="flex h-1.5 flex-1 justify-end overflow-hidden rounded-full bg-surface-2">
                        <div className="h-full rounded-full bg-primary" style={{ width: `${va}%` }} />
                      </div>
                      <div className="flex h-1.5 flex-1 overflow-hidden rounded-full bg-surface-2">
                        <div className="h-full rounded-full bg-cyan" style={{ width: `${vb}%` }} />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </GlassCard>
        </div>

        <div className="space-y-6">
          <GlassCard tilt glow="cyan" className="p-6">
            <p className="label-tech">Gemini · comparison verdict</p>
            <div className="mt-3">
              <StreamBlock text={text} streaming={!done} />
            </div>
            <div className="mt-5 rounded-xl border border-border bg-surface/50 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted-foreground">Recommendation confidence</span>
                <span className="telemetry text-sm text-cyan">86%</span>
              </div>
              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-surface-2">
                <div className="h-full w-[86%] rounded-full bg-cyan" />
              </div>
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <p className="label-tech">Watch-outs</p>
            <ul className="mt-3 space-y-2.5 text-xs leading-relaxed text-muted-foreground">
              <li>· Different disciplines — overall scores are not always comparable.</li>
              <li>· {b.name} has a smaller consistency sample than {a.name}.</li>
              <li>· Temperament under pressure is not measurable from footage alone.</li>
            </ul>
          </GlassCard>
        </div>
      </div>
    </AppLayout>
  );
}
