import { createFileRoute, Link } from "@tanstack/react-router";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { MetricBar } from "@/components/common/MetricBar";
import { useApp } from "@/context/AppContext";
import { getPlayer } from "@/data/players";
import { recentAnalyses } from "@/data/analysis";

export const Route = createFileRoute("/profile")({
  head: () => ({
    meta: [
      { title: "Profile — VisionSelect AI" },
      {
        name: "description",
        content:
          "Your VisionSelect account: role, evaluation activity, override history and the squad you currently track.",
      },
      { property: "og:title", content: "Profile — VisionSelect AI" },
      { property: "og:description", content: "Account details and evaluation activity." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Profile,
});

const roleCopy: Record<string, string> = {
  coach: "Coach · development focus",
  selector: "Selector · squad decisions",
  player: "Player · personal progress",
};

function Profile() {
  const { role, activePlayerId } = useApp();
  const player = getPlayer(activePlayerId);

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Account"
        title="Profile"
        description="Your working context — the role you evaluate in, the player you are tracking, and the decisions you have made on top of the model."
        actions={
          <Link to="/roles">
            <MagneticButton variant="outline">Change role</MagneticButton>
          </Link>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
        <GlassCard tilt glow="primary" className="p-7 text-center">
          <span className="telemetry mx-auto grid h-20 w-20 place-items-center rounded-2xl bg-primary/15 text-xl text-cyan">
            RI
          </span>
          <p className="mt-4 text-lg text-foreground">R. Iyer</p>
          <p className="text-xs text-muted-foreground">{roleCopy[role]}</p>
          <div className="mt-6 space-y-3 text-left">
            {[
              ["Organisation", "Karnataka State Academy"],
              ["Member since", "March 2024"],
              ["Tracking", player.name],
              ["Model card", "vs-cricket-3.2"],
            ].map(([k, v]) => (
              <div key={k} className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">{k}</span>
                <span className="text-foreground">{v}</span>
              </div>
            ))}
          </div>
        </GlassCard>

        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-3">
            {[
              ["Evaluations reviewed", "142"],
              ["Overrides submitted", "11"],
              ["Agreement with AI", "92%"],
            ].map(([k, v]) => (
              <GlassCard key={k} className="p-5">
                <p className="label-tech">{k}</p>
                <p className="telemetry mt-2 text-3xl text-foreground">{v}</p>
              </GlassCard>
            ))}
          </div>

          <GlassCard className="p-6">
            <h2 className="telemetry text-lg text-foreground">Currently tracking</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              {player.name} · {player.role} · last analysed {player.lastAnalyzed}
            </p>
            <div className="mt-5 space-y-4">
              <MetricBar label="Batting technique" value={player.metrics.batting} />
              <MetricBar label="Fielding" value={player.metrics.fielding} tone="cyan" />
              <MetricBar label="Consistency" value={player.metrics.consistency} tone="success" />
            </div>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link to="/results">
                <MagneticButton>Open evaluation</MagneticButton>
              </Link>
              <Link to="/report">
                <MagneticButton variant="outline">Coach report</MagneticButton>
              </Link>
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <h2 className="telemetry text-lg text-foreground">Recent activity</h2>
            <div className="mt-4 space-y-2">
              {recentAnalyses.slice(0, 5).map((r) => (
                <div
                  key={r.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-surface/40 px-4 py-3"
                >
                  <div>
                    <p className="text-sm text-foreground">{r.playerName}</p>
                    <p className="text-[11px] text-muted-foreground">
                      {r.venue} · {r.date}
                    </p>
                  </div>
                  <span className="telemetry text-sm text-cyan">{r.score}</span>
                </div>
              ))}
            </div>
          </GlassCard>
        </div>
      </div>
    </AppLayout>
  );
}
