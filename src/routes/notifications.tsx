import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { AlertTriangle, Bell, CheckCircle2, Info, Sparkle } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/notifications")({
  head: () => ({
    meta: [
      { title: "Notifications — VisionSelect AI" },
      {
        name: "description",
        content:
          "Analysis completions, low-confidence alerts, human override requests and Gemini insights across your squad.",
      },
      { property: "og:title", content: "Notifications — VisionSelect AI" },
      { property: "og:description", content: "Everything the evaluation engine wants you to know." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Notifications,
});

type Kind = "complete" | "alert" | "insight" | "info";

const ITEMS: {
  id: string;
  kind: Kind;
  title: string;
  body: string;
  time: string;
  to?: "/results" | "/explain" | "/assistant" | "/fairness" | "/compare";
  cta?: string;
}[] = [
  {
    id: "n1",
    kind: "complete",
    title: "Analysis complete — Arjun Mehra",
    body: "18:42 of footage processed, 118 events tracked. Overall score 87 at 94% confidence.",
    time: "12 min ago",
    to: "/results",
    cta: "View results",
  },
  {
    id: "n2",
    kind: "alert",
    title: "Low sample warning — bowling metric",
    body: "Only 6 deliveries bowled in this session. The bowling score has been down-weighted rather than estimated.",
    time: "12 min ago",
    to: "/explain",
    cta: "See why",
  },
  {
    id: "n3",
    kind: "insight",
    title: "Gemini flagged a correctable fault",
    body: "Back-foot defence against pace above 130 km/h accounts for −8 points and shows in-session improvement.",
    time: "38 min ago",
    to: "/assistant",
    cta: "Ask Gemini",
  },
  {
    id: "n4",
    kind: "info",
    title: "Coach override accepted",
    body: "R. Iyer disputed the consistency score for Rohan Desai. The override is logged against model card vs-cricket-3.2.",
    time: "2 hours ago",
    to: "/fairness",
    cta: "Fairness report",
  },
  {
    id: "n5",
    kind: "complete",
    title: "Comparison ready — Arjun vs Rohan",
    body: "Head-to-head profile generated with an 86% confidence recommendation.",
    time: "Yesterday",
    to: "/compare",
    cta: "Open comparison",
  },
  {
    id: "n6",
    kind: "info",
    title: "Monthly fairness audit passed",
    body: "Regional score variance held at ±1.8 points across the current cohort.",
    time: "3 days ago",
  },
];

const meta: Record<Kind, { icon: typeof Bell; tone: string; label: string }> = {
  complete: { icon: CheckCircle2, tone: "text-success bg-success/12", label: "Complete" },
  alert: { icon: AlertTriangle, tone: "text-warning bg-warning/12", label: "Alert" },
  insight: { icon: Sparkle, tone: "text-cyan bg-cyan/12", label: "Insight" },
  info: { icon: Info, tone: "text-primary bg-primary/12", label: "Info" },
};

const FILTERS = ["all", "complete", "alert", "insight", "info"] as const;

function Notifications() {
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>("all");
  const [read, setRead] = useState<string[]>([]);
  const items = ITEMS.filter((i) => filter === "all" || i.kind === filter);

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Activity"
        title="Notifications"
        description="Analysis completions, confidence warnings and human overrides — the engine tells you when it is unsure, not only when it succeeds."
        actions={
          <MagneticButton variant="outline" onClick={() => setRead(ITEMS.map((i) => i.id))}>
            Mark all read
          </MagneticButton>
        }
      />

      <div className="mb-6 flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={cn(
              "focus-ring rounded-full border px-4 py-2 text-xs capitalize transition-colors",
              filter === f
                ? "border-primary/50 bg-primary/15 text-foreground"
                : "border-border bg-surface/50 text-muted-foreground hover:text-foreground",
            )}
          >
            {f}
          </button>
        ))}
      </div>

      <div className="space-y-3">
        {items.map((n, i) => {
          const m = meta[n.kind];
          const Icon = m.icon;
          const isRead = read.includes(n.id);
          return (
            <GlassCard
              key={n.id}
              className={cn("animate-fade-up p-5", isRead && "opacity-60")}
              style={{ animationDelay: `${i * 50}ms` }}
            >
              <div className="flex gap-4">
                <span className={cn("grid h-9 w-9 shrink-0 place-items-center rounded-lg", m.tone)}>
                  <Icon className="h-4 w-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <p className="text-sm text-foreground">{n.title}</p>
                    <span className="text-[11px] text-muted-foreground">{n.time}</span>
                    {!isRead && <span className="h-1.5 w-1.5 rounded-full bg-cyan" />}
                  </div>
                  <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">{n.body}</p>
                  <div className="mt-3 flex flex-wrap items-center gap-3">
                    {n.to && (
                      <Link to={n.to}>
                        <MagneticButton variant="outline" className="px-3 py-1.5 text-xs">
                          {n.cta}
                        </MagneticButton>
                      </Link>
                    )}
                    <button
                      onClick={() =>
                        setRead((r) => (r.includes(n.id) ? r.filter((x) => x !== n.id) : [...r, n.id]))
                      }
                      className="focus-ring rounded-md text-[11px] text-muted-foreground hover:text-foreground"
                    >
                      {isRead ? "Mark unread" : "Mark read"}
                    </button>
                  </div>
                </div>
              </div>
            </GlassCard>
          );
        })}
      </div>
    </AppLayout>
  );
}
