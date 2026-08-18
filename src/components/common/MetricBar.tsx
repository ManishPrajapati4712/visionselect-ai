import { cn } from "@/lib/utils";
import { useCountUp } from "@/hooks/useCountUp";

export function MetricBar({
  label,
  value,
  tone = "primary",
  suffix = "",
}: {
  label: string;
  value: number;
  tone?: "primary" | "cyan" | "success" | "warning" | "danger";
  suffix?: string;
}) {
  const v = useCountUp(value, 1100);
  const bg: Record<string, string> = {
    primary: "bg-primary",
    cyan: "bg-cyan",
    success: "bg-success",
    warning: "bg-warning",
    danger: "bg-danger",
  };
  return (
    <div>
      <div className="flex items-baseline justify-between">
        <span className="text-sm text-muted-foreground">{label}</span>
        <span className="telemetry text-sm text-foreground">
          {Math.round(v)}
          {suffix}
        </span>
      </div>
      <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-surface-2">
        <div
          className={cn("h-full rounded-full transition-[width] duration-300", bg[tone])}
          style={{ width: `${Math.max(0, Math.min(100, v))}%` }}
        />
      </div>
    </div>
  );
}