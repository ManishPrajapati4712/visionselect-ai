import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Check, Loader2, Radar } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { StreamBlock } from "@/components/common/StreamBlock";
import { useApp } from "@/context/AppContext";
import { useReducedMotion } from "@/hooks/useReducedMotion";
import { getStages } from "@/services/analysisService";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/analysis")({
  head: () => ({
    meta: [
      { title: "AI analysis in progress — VisionSelect AI" },
      {
        name: "description",
        content:
          "Watch the vision pipeline and Gemini reason through match footage stage by stage, in the open.",
      },
      { property: "og:title", content: "AI analysis in progress — VisionSelect AI" },
      {
        property: "og:description",
        content: "A transparent processing timeline: detection, scoring, reasoning, report.",
      },
    ],
  }),
  component: AnalysisPage,
});

const stages = getStages();

function AnalysisPage() {
  const navigate = useNavigate();
  const reduced = useReducedMotion();
  const { markAnalysisComplete, uploadName } = useApp();
  const [index, setIndex] = useState(0);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (index >= stages.length) {
      setDone(true);
      markAnalysisComplete();
      return;
    }
    const ms = reduced ? 700 : (stages[index]?.durationMs ?? 1000);
    const t = setTimeout(() => setIndex((i) => i + 1), ms);
    return () => clearTimeout(t);
  }, [index, reduced, markAnalysisComplete]);

  const pct = Math.round((Math.min(index, stages.length) / stages.length) * 100);

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Step 2 · Reasoning"
        title={done ? "Analysis complete" : "Analysing footage"}
        description={`${uploadName ?? "match-session.mp4"} · 18:42 · 118 tracked events`}
        actions={
          done ? (
            <MagneticButton onClick={() => void navigate({ to: "/results" })}>
              View results
            </MagneticButton>
          ) : undefined
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1fr_1.1fr]">
        <GlassCard className="overflow-hidden p-0" glow="primary">
          <div className="relative aspect-video overflow-hidden bg-surface-2">
            <span aria-hidden className="grid-lines absolute inset-0 opacity-50" />
            {!done && !reduced && (
              <span
                aria-hidden
                className="animate-scan absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-transparent via-cyan/25 to-transparent"
              />
            )}
            <div className="absolute inset-0 grid place-items-center">
              <Radar className={cn("h-10 w-10 text-cyan", !done && !reduced && "animate-pulse")} />
            </div>
            <div className="absolute inset-x-0 bottom-0 flex items-center justify-between px-4 py-3 text-[11px] text-muted-foreground backdrop-blur-sm">
              <span className="telemetry">POSE TRACK · 17 KEYPOINTS</span>
              <span className="telemetry text-cyan">{pct}%</span>
            </div>
          </div>
          <div className="p-6">
            <div className="h-1.5 overflow-hidden rounded-full bg-surface-2">
              <div
                className="h-full rounded-full bg-primary transition-[width] duration-500"
                style={{ width: `${pct}%` }}
              />
            </div>
            <div className="mt-6 grid grid-cols-3 gap-4 text-center">
              {[
                { k: "118", v: "events" },
                { k: "27k", v: "frames" },
                { k: "94%", v: "confidence" },
              ].map((s) => (
                <div key={s.v}>
                  <p className="telemetry text-xl text-foreground">{s.k}</p>
                  <p className="text-[11px] text-muted-foreground">{s.v}</p>
                </div>
              ))}
            </div>
          </div>
        </GlassCard>

        <div className="space-y-3">
          {stages.map((s, i) => {
            const complete = i < index;
            const active = i === index;
            return (
              <GlassCard
                key={s.id}
                className={cn(
                  "p-5 transition-opacity",
                  !complete && !active && "opacity-45",
                  active && "ring-1 ring-primary/40",
                )}
              >
                <div className="flex items-start gap-3">
                  <span
                    className={cn(
                      "mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full border",
                      complete
                        ? "border-success/40 bg-success/15 text-success"
                        : active
                          ? "border-primary/40 bg-primary/15 text-cyan"
                          : "border-border text-muted-foreground",
                    )}
                  >
                    {complete ? (
                      <Check className="h-3.5 w-3.5" />
                    ) : active ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <span className="telemetry text-[10px]">{i + 1}</span>
                    )}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground">{s.label}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{s.description}</p>
                    {(complete || active) && (
                      <div className="mt-3 rounded-lg border border-border bg-surface/60 p-3">
                        <p className="label-tech mb-1.5">Gemini</p>
                        <StreamBlock text={s.geminiSnippet} streaming={active} />
                      </div>
                    )}
                  </div>
                </div>
              </GlassCard>
            );
          })}
        </div>
      </div>
    </AppLayout>
  );
}