import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { Download, Minus, Plus, Printer } from "lucide-react";
import { toast } from "sonner";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { MetricBar } from "@/components/common/MetricBar";
import { ScoreRing } from "@/components/common/ScoreRing";
import { StreamBlock } from "@/components/common/StreamBlock";
import { useStreamedText } from "@/hooks/useStreamedText";
import { trainingPlan } from "@/data/analysis";
import { useApp } from "@/context/AppContext";
import { getPlayer } from "@/data/players";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/report")({
  head: () => ({
    meta: [
      { title: "Coach report — VisionSelect AI" },
      {
        name: "description",
        content:
          "A shareable coach report: player overview, Gemini summary, strengths, weaknesses and a prioritised six-week training plan.",
      },
      { property: "og:title", content: "Coach report — VisionSelect AI" },
      {
        property: "og:description",
        content: "From evaluation to a development plan a coach can actually run.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Report,
});

const priorityTone: Record<string, string> = {
  high: "bg-danger/12 text-danger",
  medium: "bg-warning/12 text-warning",
  low: "bg-success/12 text-success",
};

function Report() {
  const { activePlayerId } = useApp();
  const player = getPlayer(activePlayerId);
  const [exporting, setExporting] = useState(false);

  const summary = `${player.name} is a ${player.age}-year-old ${player.role.toLowerCase()} scoring ${player.overallScore} with ${player.confidence}% confidence across ${player.matches} analysed matches.\n\nThe profile is defined by reliability rather than a single spectacular skill: front-foot drive execution is clean on 82% of attempts, and innings-to-innings variance is low. The development ceiling depends almost entirely on one correctable fault — back-foot defence against genuine pace — which the footage shows him partially self-correcting inside a single session.\n\nMy recommendation for the next block: two focused workstreams, not four. Short-ball throwdowns and constrained strike-rotation games should move the overall score more than a broader programme would.`;
  const { text, done } = useStreamedText(summary, true, 15);

  const download = () => {
    setExporting(true);
    window.setTimeout(() => {
      setExporting(false);
      toast.success("Report ready", {
        description: "PDF export is a UI demo in this build — no file was generated.",
      });
    }, 1400);
  };

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Coach deliverable"
        title="Coach report"
        description={`Evaluation summary and development plan for ${player.name} · generated ${player.lastAnalyzed}.`}
        actions={
          <>
            <MagneticButton variant="outline" onClick={() => toast("Print view is UI-only in this build.")}>
              <Printer className="h-4 w-4" /> Print
            </MagneticButton>
            <MagneticButton onClick={download} disabled={exporting}>
              <Download className="h-4 w-4" />
              {exporting ? "Preparing…" : "Download PDF"}
            </MagneticButton>
          </>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
        <div className="space-y-6">
          <GlassCard tilt glow="primary" className="p-7">
            <div className="grid place-items-center">
              <ScoreRing value={player.overallScore} size={190} sublabel={`${player.confidence}% confidence`} />
            </div>
            <div className="mt-6 space-y-3">
              {[
                ["Player", player.name],
                ["Age", `${player.age}`],
                ["Role", player.role],
                ["Team", player.team],
                ["Region", player.region],
                ["Batting", player.battingStyle],
                ["Bowling", player.bowlingStyle],
              ].map(([k, v]) => (
                <div key={k} className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">{k}</span>
                  <span className="text-foreground">{v}</span>
                </div>
              ))}
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <h2 className="telemetry text-lg text-foreground">Metric profile</h2>
            <div className="mt-5 space-y-4">
              <MetricBar label="Batting technique" value={player.metrics.batting} />
              <MetricBar label="Bowling" value={player.metrics.bowling} tone="warning" />
              <MetricBar label="Fielding" value={player.metrics.fielding} tone="cyan" />
              <MetricBar label="Fitness" value={player.metrics.fitness} tone="success" />
              <MetricBar label="Consistency" value={player.metrics.consistency} tone="cyan" />
            </div>
          </GlassCard>
        </div>

        <div className="space-y-6">
          <GlassCard glow="cyan" className="p-6">
            <p className="label-tech">Gemini · evaluation summary</p>
            <div className="mt-3">
              <StreamBlock text={text} streaming={!done} />
            </div>
          </GlassCard>

          <div className="grid gap-6 lg:grid-cols-2">
            <GlassCard className="p-6">
              <div className="flex items-center gap-2">
                <span className="grid h-7 w-7 place-items-center rounded-md bg-success/15">
                  <Plus className="h-4 w-4 text-success" />
                </span>
                <h2 className="telemetry text-lg text-foreground">Strengths</h2>
              </div>
              <ul className="mt-4 space-y-3">
                {player.strengths.map((s) => (
                  <li
                    key={s}
                    className="rounded-xl border border-success/25 bg-success/[0.06] p-4 text-xs leading-relaxed text-muted-foreground"
                  >
                    {s}
                  </li>
                ))}
              </ul>
            </GlassCard>

            <GlassCard className="p-6">
              <div className="flex items-center gap-2">
                <span className="grid h-7 w-7 place-items-center rounded-md bg-danger/15">
                  <Minus className="h-4 w-4 text-danger" />
                </span>
                <h2 className="telemetry text-lg text-foreground">Weaknesses</h2>
              </div>
              <ul className="mt-4 space-y-3">
                {player.weaknesses.map((w) => (
                  <li
                    key={w}
                    className="rounded-xl border border-danger/25 bg-danger/[0.06] p-4 text-xs leading-relaxed text-muted-foreground"
                  >
                    {w}
                  </li>
                ))}
              </ul>
            </GlassCard>
          </div>

          <GlassCard className="p-6">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h2 className="telemetry text-lg text-foreground">Gemini training plan</h2>
                <p className="mt-1 text-xs text-muted-foreground">
                  Six weeks, ordered by measured score impact.
                </p>
              </div>
              <Link to="/assistant">
                <MagneticButton variant="outline">Discuss this plan</MagneticButton>
              </Link>
            </div>
            <div className="mt-5 space-y-3">
              {trainingPlan.map((t, i) => (
                <div
                  key={t.id}
                  className="animate-fade-up rounded-xl border border-border bg-surface/40 p-4"
                  style={{ animationDelay: `${i * 60}ms` }}
                >
                  <div className="flex flex-wrap items-center gap-3">
                    <span className="telemetry grid h-7 w-7 place-items-center rounded-md bg-surface-2 text-xs text-cyan">
                      {i + 1}
                    </span>
                    <span className="flex-1 text-sm text-foreground">{t.focus}</span>
                    <span
                      className={cn(
                        "rounded-full px-2.5 py-1 text-[10px] uppercase tracking-wider",
                        priorityTone[t.priority],
                      )}
                    >
                      {t.priority}
                    </span>
                  </div>
                  <p className="mt-2 text-xs leading-relaxed text-muted-foreground">{t.drill}</p>
                  <div className="mt-3 flex flex-wrap gap-4 text-[11px] text-muted-foreground">
                    <span className="telemetry text-cyan">{t.frequency}</span>
                    <span className="telemetry text-success">{t.expectedGain}</span>
                  </div>
                </div>
              ))}
            </div>
          </GlassCard>
        </div>
      </div>
    </AppLayout>
  );
}
