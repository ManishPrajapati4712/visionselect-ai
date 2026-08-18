import { cn } from "@/lib/utils";
import { useCountUp } from "@/hooks/useCountUp";

interface ScoreRingProps {
  value: number;
  size?: number;
  stroke?: number;
  label?: string;
  sublabel?: string;
  start?: boolean;
  tone?: "primary" | "cyan" | "success" | "warning";
  className?: string;
}

const toneVar: Record<string, string> = {
  primary: "var(--primary)",
  cyan: "var(--cyan)",
  success: "var(--success)",
  warning: "var(--warning)",
};

/** Signature VisionSelect telemetry ring: ticked outer bezel + animated arc. */
export function ScoreRing({
  value,
  size = 220,
  stroke = 12,
  label = "AI Score",
  sublabel,
  start = true,
  tone = "primary",
  className,
}: ScoreRingProps) {
  const animated = useCountUp(value, 1600, start);
  const r = (size - stroke * 2) / 2;
  const c = 2 * Math.PI * r;
  const pct = Math.max(0, Math.min(100, animated)) / 100;
  const ticks = Array.from({ length: 60 });

  return (
    <div
      className={cn("relative inline-grid place-items-center", className)}
      style={{ width: size, height: size }}
    >
      <svg width={size} height={size} className="-rotate-90" aria-hidden>
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="var(--surface-2)"
          strokeWidth={stroke}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke={toneVar[tone]}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={c}
          strokeDashoffset={c * (1 - pct)}
          style={{ filter: "drop-shadow(0 0 12px oklch(0.66 0.19 256 / 55%))" }}
        />
      </svg>

      <svg
        width={size}
        height={size}
        className="absolute inset-0 opacity-60"
        aria-hidden
        viewBox={`0 0 ${size} ${size}`}
      >
        {ticks.map((_, i) => {
          const a = (i / ticks.length) * Math.PI * 2 - Math.PI / 2;
          const outer = size / 2 - 1;
          const inner = outer - (i % 5 === 0 ? 9 : 4);
          const cx = size / 2;
          return (
            <line
              key={i}
              x1={cx + Math.cos(a) * inner}
              y1={cx + Math.sin(a) * inner}
              x2={cx + Math.cos(a) * outer}
              y2={cx + Math.sin(a) * outer}
              stroke={i / ticks.length <= pct ? toneVar[tone] : "var(--surface-2)"}
              strokeWidth={i % 5 === 0 ? 1.6 : 1}
            />
          );
        })}
      </svg>

      <div className="absolute grid place-items-center text-center">
        <span className="label-tech">{label}</span>
        <span
          className="telemetry leading-none text-foreground"
          style={{ fontSize: size * 0.3 }}
        >
          {Math.round(animated)}
        </span>
        {sublabel && <span className="mt-1 text-xs text-muted-foreground">{sublabel}</span>}
      </div>
    </div>
  );
}