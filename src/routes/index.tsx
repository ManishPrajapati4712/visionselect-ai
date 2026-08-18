import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ArrowRight,
  Braces,
  CheckCircle2,
  Scale as ScaleIcon,
  ScanLine,
  Sparkle,
  Timer,
  Video,
} from "lucide-react";
import { Logo } from "@/components/brand/Logo";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { ScoreRing } from "@/components/common/ScoreRing";
import { useSpotlight } from "@/hooks/useSpotlight";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "VisionSelect AI — Explainable Cricket Talent Intelligence" },
      {
        name: "description",
        content:
          "Score cricket talent from match video, then see the timestamped evidence behind every point. Explainable, auditable, human-in-the-loop.",
      },
      { property: "og:title", content: "VisionSelect AI — Explainable Cricket Talent Intelligence" },
      {
        property: "og:description",
        content:
          "AI that shows why the score exists — evidence timelines, fairness reports and a Gemini analyst you can question.",
      },
    ],
  }),
  component: Landing,
});

const steps = [
  {
    icon: Video,
    title: "Upload match footage",
    body: "Drop in a session or match video. Frames are sampled, stabilised and tracked at 24 fps.",
  },
  {
    icon: ScanLine,
    title: "Vision model scores the play",
    body: "Pose keypoints, bat tracking and event detection produce five metric families — never a black box number.",
  },
  {
    icon: Sparkle,
    title: "Gemini explains the reasoning",
    body: "A live reasoning stream ties each point of the score to a timestamp you can jump to and challenge.",
  },
];

function Landing() {
  const spotlight = useSpotlight<HTMLDivElement>();

  return (
    <main className="min-h-screen bg-background">
      <header className="sticky top-0 z-30 border-b border-border bg-background/70 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-5">
          <Logo />
          <nav className="flex items-center gap-2">
            <Link
              to="/fairness"
              className="focus-ring hidden rounded-lg px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground sm:block"
            >
              Fairness
            </Link>
            <Link
              to="/explain"
              className="focus-ring hidden rounded-lg px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground sm:block"
            >
              Explainability
            </Link>
            <Link to="/roles">
              <MagneticButton>Enter platform</MagneticButton>
            </Link>
          </nav>
        </div>
      </header>

      <section
        ref={spotlight}
        className="hero-bg relative overflow-hidden border-b border-border"
      >
        <span aria-hidden className="grid-lines pointer-events-none absolute inset-0 opacity-40" />
        <span
          aria-hidden
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              "radial-gradient(520px circle at var(--mx, 50%) var(--my, 20%), oklch(1 0 0 / 5%), transparent 60%)",
          }}
        />
        <div className="relative mx-auto grid max-w-7xl items-center gap-14 px-5 py-20 lg:grid-cols-[1.15fr_1fr] lg:py-28">
          <div className="animate-fade-up">
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-surface/60 px-3 py-1.5 text-xs text-muted-foreground">
              <span className="h-1.5 w-1.5 rounded-full bg-cyan" />
              Powered by Gemini · Cricket talent intelligence
            </span>
            <h1 className="telemetry mt-6 text-4xl leading-[1.05] text-foreground sm:text-6xl">
              Don&apos;t just score the player.
              <span className="block text-gradient">Show why the score exists.</span>
            </h1>
            <p className="mt-6 max-w-xl text-base leading-relaxed text-muted-foreground">
              VisionSelect AI evaluates cricket talent from match video and returns a timestamped
              evidence trail for every point gained or lost — so coaches and selectors can question
              the model, not just accept it.
            </p>
            <div className="mt-9 flex flex-wrap gap-3">
              <Link to="/upload">
                <MagneticButton size="lg">
                  Upload Match Video <ArrowRight className="h-4 w-4" />
                </MagneticButton>
              </Link>
              <Link to="/assistant">
                <MagneticButton size="lg" variant="outline">
                  Analyze with Gemini
                </MagneticButton>
              </Link>
            </div>
            <dl className="mt-12 grid max-w-lg grid-cols-3 gap-6 border-t border-border pt-8">
              {[
                { k: "118", v: "tracked events per match" },
                { k: "47", v: "features, 0 protected" },
                { k: "94%", v: "median confidence" },
              ].map((s) => (
                <div key={s.v}>
                  <dt className="telemetry text-2xl text-foreground">{s.k}</dt>
                  <dd className="mt-1 text-xs leading-snug text-muted-foreground">{s.v}</dd>
                </div>
              ))}
            </dl>
          </div>

          <GlassCard tilt className="animate-fade-up p-8" glow="primary">
            <div className="flex items-center justify-between">
              <span className="label-tech">Live evaluation</span>
              <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <Timer className="h-3.5 w-3.5" /> 18:42 footage
              </span>
            </div>
            <div className="mt-6 grid place-items-center">
              <ScoreRing value={87} sublabel="Arjun Mehra · 94% confidence" />
            </div>
            <div className="mt-8 space-y-2.5">
              {[
                { t: "04:12", l: "Excellent cover drive", v: "+12", up: true },
                { t: "02:31", l: "Poor footwork vs short ball", v: "−8", up: false },
                { t: "08:39", l: "Boundary save at deep midwicket", v: "+9", up: true },
              ].map((e) => (
                <div
                  key={e.t}
                  className="flex items-center gap-3 rounded-lg border border-border bg-surface/50 px-3 py-2.5 text-sm"
                >
                  <span className="telemetry text-xs text-cyan">{e.t}</span>
                  <span className="flex-1 truncate text-foreground/85">{e.l}</span>
                  <span
                    className={`telemetry text-sm ${e.up ? "text-success" : "text-danger"}`}
                  >
                    {e.v}
                  </span>
                </div>
              ))}
            </div>
          </GlassCard>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-5 py-20">
        <p className="label-tech">How it works</p>
        <h2 className="telemetry mt-3 text-3xl text-foreground">Video in, reasoning out</h2>
        <div className="mt-10 grid gap-5 md:grid-cols-3">
          {steps.map((s, i) => (
            <GlassCard key={s.title} tilt className="lift p-6">
              <div className="flex items-center justify-between">
                <s.icon className="h-5 w-5 text-cyan" />
                <span className="telemetry text-xs text-muted-foreground">0{i + 1}</span>
              </div>
              <h3 className="mt-5 text-base font-semibold text-foreground">{s.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{s.body}</p>
            </GlassCard>
          ))}
        </div>
      </section>

      <section className="border-y border-border bg-surface/30">
        <div className="mx-auto grid max-w-7xl gap-5 px-5 py-20 lg:grid-cols-2">
          <GlassCard className="p-8">
            <Braces className="h-5 w-5 text-cyan" />
            <h3 className="telemetry mt-5 text-2xl text-foreground">Explainable by construction</h3>
            <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
              Every score decomposes into timestamped evidence with its own confidence value. Click
              an entry to jump to the moment in the footage that produced it.
            </p>
            <Link
              to="/explain"
              className="focus-ring mt-6 inline-flex items-center gap-2 text-sm text-cyan"
            >
              See the evidence timeline <ArrowRight className="h-4 w-4" />
            </Link>
          </GlassCard>
          <GlassCard className="p-8">
            <ScaleIcon className="h-5 w-5 text-cyan" />
            <h3 className="telemetry mt-5 text-2xl text-foreground">Fair, and provably so</h3>
            <ul className="mt-4 space-y-2 text-sm text-muted-foreground">
              {["Technique", "Performance", "Consistency", "Accuracy"].map((f) => (
                <li key={f} className="flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-success" /> Evaluated on {f.toLowerCase()}
                </li>
              ))}
            </ul>
            <p className="mt-4 text-sm text-muted-foreground">
              Never on popularity, reputation or background — 0 of 47 active features encode a
              protected attribute.
            </p>
            <Link
              to="/fairness"
              className="focus-ring mt-6 inline-flex items-center gap-2 text-sm text-cyan"
            >
              Read the fairness report <ArrowRight className="h-4 w-4" />
            </Link>
          </GlassCard>
        </div>
      </section>

      <footer className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-5 py-10 text-xs text-muted-foreground">
        <Logo />
        <p>AI assists human decision-makers — it does not replace them.</p>
      </footer>
    </main>
  );
}