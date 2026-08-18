import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Clock, Play, ShieldCheck, TrendingDown, TrendingUp } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { AgreeDisagreeReview } from "@/components/common/AgreeDisagreeReview";
import { MetricBar } from "@/components/common/MetricBar";
import { StreamBlock } from "@/components/common/StreamBlock";
import { useStreamedText } from "@/hooks/useStreamedText";
import { getContributions, getEvidence } from "@/services/analysisService";
import { useApp } from "@/context/AppContext";
import { getPlayer } from "@/data/players";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/explain")({
  head: () => ({
    meta: [
      { title: "Why this score — VisionSelect AI" },
      {
        name: "description",
        content:
          "Every point traced to a timestamped moment in the footage: evidence timeline, score impact and confidence for each signal.",
      },
      { property: "og:title", content: "Why this score — VisionSelect AI" },
      {
        property: "og:description",
        content: "Explainable AI for cricket talent evaluation, evidence by evidence.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Explain,
});

const NARRATIVE =
  "This score is not a verdict handed down from a black box — it is the sum of 118 tracked events, each anchored to a moment you can watch.\n\nThe largest single positive contribution came at 04:12: a cover drive with the front elbow high and contact under the eyeline, worth +12 points to the batting dimension. The largest negative came at 02:31, where the back foot stayed planted across four short deliveries, costing −8.\n\nWhere I am less certain, I say so. The late-innings shot-selection entry rests on only two deliveries and carries 68% confidence, so it is weighted accordingly rather than treated as fact.";

function Explain() {
  const evidence = getEvidence();
  const contributions = getContributions();
  const { activePlayerId } = useApp();
  const player = getPlayer(activePlayerId);
  const [activeId, setActiveId] = useState(evidence[2]!.id);
  const [jumping, setJumping] = useState(false);
  const active = evidence.find((e) => e.id === activeId)!;
  const { text, done } = useStreamedText(NARRATIVE, true, 18);

  const jump = (id: string) => {
    setActiveId(id);
    setJumping(true);
    window.setTimeout(() => setJumping(false), 900);
  };

  const totalSeconds = 1122;

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Explainable AI"
        title="Why this score?"
        description={`Each point in ${player.name}'s score of ${player.overallScore} is traced to a timestamped moment in the footage. Select any event to jump the video and see its measured impact.`}
        actions={
          <>
            <Link to="/fairness">
              <MagneticButton variant="outline">Fairness report</MagneticButton>
            </Link>
            <Link to="/assistant">
              <MagneticButton>Ask Gemini</MagneticButton>
            </Link>
          </>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[1fr_400px]">
        <div className="space-y-6">
          <GlassCard glow="cyan" className="overflow-hidden p-0">
            <div className="relative aspect-video bg-surface-2/60">
              <div
                aria-hidden
                className="absolute inset-0 opacity-70"
                style={{
                  background:
                    "radial-gradient(circle at 50% 40%, oklch(0.66 0.19 256 / 22%), transparent 65%)",
                }}
              />
              <div className="absolute inset-0 grid place-items-center text-center">
                <div>
                  <span
                    className={cn(
                      "grid h-16 w-16 place-items-center rounded-full border border-cyan/40 bg-background/60 backdrop-blur transition-transform duration-500",
                      jumping && "scale-110",
                    )}
                  >
                    <Play className="h-6 w-6 translate-x-0.5 text-cyan" />
                  </span>
                  <p className="telemetry mt-4 text-2xl text-foreground">{active.timestamp}</p>
                  <p className="mt-1 max-w-sm px-6 text-xs text-muted-foreground">
                    {jumping ? "Seeking to evidence marker…" : active.title}
                  </p>
                </div>
              </div>
              <div className="absolute inset-x-0 bottom-0 p-4">
                <div className="relative h-1.5 rounded-full bg-surface-2">
                  <div
                    className="absolute inset-y-0 left-0 rounded-full bg-cyan transition-[width] duration-500"
                    style={{ width: `${(active.seconds / totalSeconds) * 100}%` }}
                  />
                  {evidence.map((e) => (
                    <button
                      key={e.id}
                      onClick={() => jump(e.id)}
                      aria-label={`Jump to ${e.timestamp}`}
                      className={cn(
                        "focus-ring absolute -top-1 h-3.5 w-3.5 -translate-x-1/2 rounded-full border transition-transform hover:scale-125",
                        e.impact >= 0
                          ? "border-success/60 bg-success/70"
                          : "border-danger/60 bg-danger/70",
                        e.id === activeId && "scale-125 ring-2 ring-cyan/50",
                      )}
                      style={{ left: `${(e.seconds / totalSeconds) * 100}%` }}
                    />
                  ))}
                </div>
                <div className="mt-2 flex justify-between text-[11px] text-muted-foreground">
                  <span className="telemetry">00:00</span>
                  <span className="telemetry">18:42</span>
                </div>
              </div>
            </div>

            <div className="border-t border-border p-6">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="label-tech">Selected evidence · {active.timestamp}</p>
                  <h2 className="telemetry mt-1 text-lg text-foreground">{active.title}</h2>
                </div>
                <span
                  className={cn(
                    "telemetry inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm",
                    active.impact >= 0
                      ? "bg-success/12 text-success"
                      : "bg-danger/12 text-danger",
                  )}
                >
                  {active.impact >= 0 ? (
                    <TrendingUp className="h-4 w-4" />
                  ) : (
                    <TrendingDown className="h-4 w-4" />
                  )}
                  {active.impact > 0 ? "+" : ""}
                  {active.impact} pts
                </span>
              </div>
              <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{active.detail}</p>
              <div className="mt-5">
                <MetricBar label="Model confidence" value={active.confidence} tone="cyan" suffix="%" />
              </div>
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <h2 className="telemetry text-lg text-foreground">Evidence timeline</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              8 scoring events extracted from 18:42 of footage.
            </p>
            <ol className="relative mt-6 space-y-3 border-l border-border pl-6">
              {evidence.map((e, i) => (
                <li key={e.id} className="animate-fade-up" style={{ animationDelay: `${i * 55}ms` }}>
                  <span
                    className={cn(
                      "absolute -left-[7px] mt-4 h-3.5 w-3.5 rounded-full border-2 border-background",
                      e.impact >= 0 ? "bg-success" : "bg-danger",
                    )}
                  />
                  <button
                    onClick={() => jump(e.id)}
                    className={cn(
                      "focus-ring w-full rounded-xl border p-4 text-left transition-colors",
                      e.id === activeId
                        ? "border-primary/40 bg-surface-2/70"
                        : "border-border bg-surface/40 hover:border-primary/25 hover:bg-surface-2/50",
                    )}
                  >
                    <div className="flex flex-wrap items-center gap-3">
                      <span className="telemetry inline-flex items-center gap-1.5 rounded-md bg-surface-2 px-2 py-1 text-xs text-cyan">
                        <Clock className="h-3 w-3" />
                        {e.timestamp}
                      </span>
                      <span className="flex-1 text-sm text-foreground">{e.title}</span>
                      <span
                        className={cn(
                          "telemetry text-sm",
                          e.impact >= 0 ? "text-success" : "text-danger",
                        )}
                      >
                        {e.impact > 0 ? "+" : ""}
                        {e.impact}
                      </span>
                    </div>
                    <p className="mt-2 text-xs leading-relaxed text-muted-foreground">{e.detail}</p>
                    <div className="mt-3 flex items-center gap-2">
                      <div className="h-1 flex-1 overflow-hidden rounded-full bg-surface-2">
                        <div
                          className="h-full rounded-full bg-cyan/70"
                          style={{ width: `${e.confidence}%` }}
                        />
                      </div>
                      <span className="telemetry text-[11px] text-muted-foreground">
                        {e.confidence}% confidence
                      </span>
                    </div>
                  </button>
                </li>
              ))}
            </ol>
          </GlassCard>

          <AgreeDisagreeReview context="this evidence timeline" />
        </div>

        <div className="space-y-6">
          <GlassCard tilt glow="primary" className="p-6">
            <p className="label-tech">Gemini · reasoning</p>
            <div className="mt-3">
              <StreamBlock text={text} streaming={!done} />
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <h2 className="telemetry text-lg text-foreground">Metric contribution</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              How each dimension moved the final score, after weighting.
            </p>
            <div className="mt-5 space-y-4">
              {contributions.map((c) => (
                <div key={c.metric} className="rounded-xl border border-border bg-surface/40 p-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-foreground">{c.label}</span>
                    <span
                      className={cn(
                        "telemetry text-sm",
                        c.contribution >= 0 ? "text-success" : "text-danger",
                      )}
                    >
                      {c.contribution > 0 ? "+" : ""}
                      {c.contribution}
                    </span>
                  </div>
                  <div className="mt-2 flex h-1.5 overflow-hidden rounded-full bg-surface-2">
                    <div
                      className={cn(
                        "h-full rounded-full",
                        c.contribution >= 0 ? "bg-primary" : "bg-danger",
                      )}
                      style={{ width: `${Math.min(100, Math.abs(c.contribution) * 5)}%` }}
                    />
                  </div>
                  <p className="mt-2 text-[11px] leading-relaxed text-muted-foreground">
                    Weight {Math.round(c.weight * 100)}% · {c.note}
                  </p>
                </div>
              ))}
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <div className="flex items-start gap-3">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-success" />
              <div>
                <p className="text-sm text-foreground">Human override available</p>
                <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                  This evaluation assists selection, it does not decide it. Coaches can dispute any
                  evidence entry and the override is logged against the model card.
                </p>
                <Link to="/report" className="focus-ring mt-3 inline-block">
                  <MagneticButton variant="outline">Open coach report</MagneticButton>
                </Link>
              </div>
            </div>
          </GlassCard>
        </div>
      </div>
    </AppLayout>
  );
}
