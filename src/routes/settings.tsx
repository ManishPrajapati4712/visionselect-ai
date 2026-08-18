import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { toast } from "sonner";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { useApp } from "@/context/AppContext";
import type { Role } from "@/types";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/settings")({
  head: () => ({
    meta: [
      { title: "Settings — VisionSelect AI" },
      {
        name: "description",
        content:
          "Control evaluation defaults, confidence thresholds, explanation depth and motion preferences for VisionSelect AI.",
      },
      { property: "og:title", content: "Settings — VisionSelect AI" },
      { property: "og:description", content: "Tune how the evaluation engine behaves and explains itself." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: SettingsPage,
});

function Toggle({
  label,
  description,
  value,
  onChange,
}: {
  label: string;
  description: string;
  value: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="flex items-start justify-between gap-6 rounded-xl border border-border bg-surface/40 p-4">
      <div>
        <p className="text-sm text-foreground">{label}</p>
        <p className="mt-1 text-xs leading-relaxed text-muted-foreground">{description}</p>
      </div>
      <button
        role="switch"
        aria-checked={value}
        aria-label={label}
        onClick={() => onChange(!value)}
        className={cn(
          "focus-ring mt-1 h-6 w-11 shrink-0 rounded-full border transition-colors",
          value ? "border-primary/60 bg-primary/70" : "border-border bg-surface-2",
        )}
      >
        <span
          className={cn(
            "block h-4 w-4 rounded-full bg-foreground transition-transform",
            value ? "translate-x-6" : "translate-x-1",
          )}
        />
      </button>
    </div>
  );
}

const ROLES: { id: Role; label: string }[] = [
  { id: "coach", label: "Coach" },
  { id: "selector", label: "Selector" },
  { id: "player", label: "Player" },
];

function SettingsPage() {
  const { role, setRole } = useApp();
  const [threshold, setThreshold] = useState(80);
  const [depth, setDepth] = useState<"concise" | "standard" | "forensic">("standard");
  const [flags, setFlags] = useState({
    lowSample: true,
    autoExplain: true,
    reducedMotion: false,
    emailDigest: false,
  });

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Preferences"
        title="Settings"
        description="These controls change how the engine reports itself — not how it scores. Evaluation logic is fixed and versioned to the model card."
        actions={
          <MagneticButton onClick={() => toast.success("Preferences saved for this session.")}>
            Save changes
          </MagneticButton>
        }
      />

      <div className="grid gap-6 xl:grid-cols-2">
        <GlassCard glow="primary" className="p-6">
          <h2 className="telemetry text-lg text-foreground">Default role</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Sets which dashboard and language the product opens with.
          </p>
          <div className="mt-4 grid grid-cols-3 gap-2">
            {ROLES.map((r) => (
              <button
                key={r.id}
                onClick={() => setRole(r.id)}
                className={cn(
                  "focus-ring rounded-xl border px-3 py-3 text-sm transition-colors",
                  role === r.id
                    ? "border-primary/50 bg-primary/15 text-foreground"
                    : "border-border bg-surface/50 text-muted-foreground hover:text-foreground",
                )}
              >
                {r.label}
              </button>
            ))}
          </div>
        </GlassCard>

        <GlassCard className="p-6">
          <h2 className="telemetry text-lg text-foreground">Confidence threshold</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            Evaluations below this level are surfaced for human review before they can inform a
            selection decision.
          </p>
          <div className="mt-6 flex items-center gap-4">
            <input
              type="range"
              min={50}
              max={99}
              value={threshold}
              onChange={(e) => setThreshold(Number(e.target.value))}
              aria-label="Confidence threshold"
              className="focus-ring h-1.5 flex-1 cursor-pointer appearance-none rounded-full bg-surface-2 accent-[var(--primary)]"
            />
            <span className="telemetry w-14 text-right text-lg text-cyan">{threshold}%</span>
          </div>
        </GlassCard>

        <GlassCard className="p-6">
          <h2 className="telemetry text-lg text-foreground">Explanation depth</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            How much reasoning Gemini writes alongside every score.
          </p>
          <div className="mt-4 space-y-2">
            {(
              [
                ["concise", "One paragraph verdict with the headline evidence."],
                ["standard", "Verdict, key evidence entries and stated confidence."],
                ["forensic", "Full evidence chain, weights, caveats and sample sizes."],
              ] as const
            ).map(([id, desc]) => (
              <button
                key={id}
                onClick={() => setDepth(id)}
                className={cn(
                  "focus-ring w-full rounded-xl border p-4 text-left transition-colors",
                  depth === id
                    ? "border-primary/50 bg-primary/10"
                    : "border-border bg-surface/40 hover:border-primary/30",
                )}
              >
                <p className="text-sm capitalize text-foreground">{id}</p>
                <p className="mt-1 text-xs text-muted-foreground">{desc}</p>
              </button>
            ))}
          </div>
        </GlassCard>

        <GlassCard className="p-6">
          <h2 className="telemetry text-lg text-foreground">Behaviour</h2>
          <div className="mt-4 space-y-3">
            <Toggle
              label="Flag low-sample metrics"
              description="Warn when a score is built from too few tracked events to be reliable."
              value={flags.lowSample}
              onChange={(v) => setFlags((f) => ({ ...f, lowSample: v }))}
            />
            <Toggle
              label="Auto-generate explanations"
              description="Write the Why This Score breakdown as soon as an analysis completes."
              value={flags.autoExplain}
              onChange={(v) => setFlags((f) => ({ ...f, autoExplain: v }))}
            />
            <Toggle
              label="Reduce motion"
              description="Disable tilt, streaming caret and count-up animations."
              value={flags.reducedMotion}
              onChange={(v) => setFlags((f) => ({ ...f, reducedMotion: v }))}
            />
            <Toggle
              label="Weekly email digest"
              description="A Monday summary of new evaluations and pending overrides."
              value={flags.emailDigest}
              onChange={(v) => setFlags((f) => ({ ...f, emailDigest: v }))}
            />
          </div>
        </GlassCard>
      </div>

      <p className="mt-6 text-center text-[11px] text-muted-foreground">
        Frontend demo build — preferences are held in memory for this session only.
      </p>
    </AppLayout>
  );
}
