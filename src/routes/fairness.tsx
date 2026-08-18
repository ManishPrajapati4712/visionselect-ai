import { createFileRoute, Link } from "@tanstack/react-router";
import { Check, ShieldCheck, X } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { ScoreRing } from "@/components/common/ScoreRing";
import { StreamBlock } from "@/components/common/StreamBlock";
import { useStreamedText } from "@/hooks/useStreamedText";
import { FAIRNESS_SCORE, getFairnessAudit, getFairnessFactors } from "@/services/analysisService";
import { fairnessNarrative } from "@/services/geminiService";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/fairness")({
  head: () => ({
    meta: [
      { title: "Fairness report — VisionSelect AI" },
      {
        name: "description",
        content:
          "What the evaluation is based on, what it is never based on, and the audit numbers that prove it — responsible AI for talent selection.",
      },
      { property: "og:title", content: "Fairness report — VisionSelect AI" },
      {
        property: "og:description",
        content: "Bias controls, audit metrics and a plain-language responsible AI explanation.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Fairness,
});

function Fairness() {
  const factors = getFairnessFactors();
  const audit = getFairnessAudit();
  const included = factors.filter((f) => f.included);
  const excluded = factors.filter((f) => !f.included);
  const { text, done } = useStreamedText(fairnessNarrative, true, 16);

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Responsible AI"
        title="Fairness report"
        description="Talent should be measured by what happens on the field. This report shows exactly which signals reach the model — and which are removed before it ever sees a player."
        actions={
          <Link to="/explain">
            <MagneticButton variant="outline">See the evidence</MagneticButton>
          </Link>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
        <GlassCard tilt glow="cyan" className="p-7">
          <div className="grid place-items-center">
            <ScoreRing
              value={FAIRNESS_SCORE}
              tone="success"
              label="Fairness"
              sublabel="Audited Aug 1, 2026"
            />
          </div>
          <div className="mt-7 space-y-3">
            {audit.map((a) => (
              <div
                key={a.label}
                className="flex items-center justify-between rounded-lg border border-border bg-surface/50 p-3"
              >
                <span className="text-xs text-muted-foreground">{a.label}</span>
                <span
                  className={cn(
                    "telemetry text-sm",
                    a.tone === "success" ? "text-success" : "text-cyan",
                  )}
                >
                  {a.value}
                </span>
              </div>
            ))}
          </div>
        </GlassCard>

        <div className="space-y-6">
          <GlassCard glow="primary" className="p-6">
            <p className="label-tech">Gemini · responsible AI explanation</p>
            <div className="mt-3">
              <StreamBlock text={text} streaming={!done} />
            </div>
          </GlassCard>

          <div className="grid gap-6 lg:grid-cols-2">
            <GlassCard className="p-6">
              <div className="flex items-center gap-2">
                <span className="grid h-7 w-7 place-items-center rounded-md bg-success/15">
                  <Check className="h-4 w-4 text-success" />
                </span>
                <h2 className="telemetry text-lg text-foreground">Evaluation is based on</h2>
              </div>
              <div className="mt-5 space-y-3">
                {included.map((f, i) => (
                  <div
                    key={f.label}
                    className="animate-fade-up rounded-xl border border-success/25 bg-success/[0.06] p-4"
                    style={{ animationDelay: `${i * 60}ms` }}
                  >
                    <p className="text-sm text-foreground">{f.label}</p>
                    <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                      {f.description}
                    </p>
                  </div>
                ))}
              </div>
            </GlassCard>

            <GlassCard className="p-6">
              <div className="flex items-center gap-2">
                <span className="grid h-7 w-7 place-items-center rounded-md bg-danger/15">
                  <X className="h-4 w-4 text-danger" />
                </span>
                <h2 className="telemetry text-lg text-foreground">Never based on</h2>
              </div>
              <div className="mt-5 space-y-3">
                {excluded.map((f, i) => (
                  <div
                    key={f.label}
                    className="animate-fade-up rounded-xl border border-danger/25 bg-danger/[0.06] p-4"
                    style={{ animationDelay: `${i * 60}ms` }}
                  >
                    <p className="text-sm text-foreground line-through decoration-danger/50">
                      {f.label}
                    </p>
                    <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                      {f.description}
                    </p>
                  </div>
                ))}
              </div>
            </GlassCard>
          </div>

          <GlassCard className="p-6">
            <div className="flex items-start gap-3">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-success" />
              <div>
                <p className="text-sm text-foreground">Humans stay in the loop</p>
                <p className="mt-1 text-xs leading-relaxed text-muted-foreground">
                  VisionSelect AI produces recommendations, never decisions. Every score can be
                  disputed, overridden and re-reviewed by a coach or selection panel, and each
                  override is recorded against model card vs-cricket-3.2.
                </p>
              </div>
            </div>
          </GlassCard>
        </div>
      </div>
    </AppLayout>
  );
}
