import { cn } from "@/lib/utils";

export function Logo({ className, compact = false }: { className?: string; compact?: boolean }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span className="relative grid h-9 w-9 place-items-center rounded-lg bg-surface-2 ring-1 ring-border">
        <svg viewBox="0 0 32 32" className="h-5 w-5" aria-hidden>
          <circle
            cx="16"
            cy="16"
            r="12"
            fill="none"
            stroke="var(--primary)"
            strokeWidth="2.5"
            strokeDasharray="56 20"
            strokeLinecap="round"
          />
          <circle cx="16" cy="16" r="4.5" fill="var(--cyan)" />
        </svg>
      </span>
      {!compact && (
        <span className="telemetry text-[15px] tracking-tight text-foreground">
          VisionSelect<span className="text-gradient"> AI</span>
        </span>
      )}
    </span>
  );
}