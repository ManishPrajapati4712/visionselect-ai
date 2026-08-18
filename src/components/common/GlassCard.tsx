import { cn } from "@/lib/utils";
import { useRef, type HTMLAttributes, type ReactNode } from "react";
import { useReducedMotion } from "@/hooks/useReducedMotion";

interface GlassCardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  /** Enables subtle 3D tilt + reflection shift on pointer move. */
  tilt?: boolean;
  glow?: "primary" | "cyan" | "none";
}

export function GlassCard({
  children,
  className,
  tilt = false,
  glow = "none",
  ...rest
}: GlassCardProps) {
  const ref = useRef<HTMLDivElement>(null);
  const reduced = useReducedMotion();

  const handleMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const el = ref.current;
    if (!el || !tilt || reduced) return;
    const r = el.getBoundingClientRect();
    const px = (e.clientX - r.left) / r.width;
    const py = (e.clientY - r.top) / r.height;
    el.style.setProperty("--mx", `${e.clientX - r.left}px`);
    el.style.setProperty("--my", `${e.clientY - r.top}px`);
    el.style.transform = `perspective(900px) rotateX(${(0.5 - py) * 5}deg) rotateY(${(px - 0.5) * 5}deg) translateY(-2px)`;
  };

  const reset = () => {
    const el = ref.current;
    if (el) el.style.transform = "";
  };

  return (
    <div
      ref={ref}
      onPointerMove={handleMove}
      onPointerLeave={reset}
      className={cn(
        "glass group/card relative overflow-hidden rounded-2xl transition-[transform,box-shadow,border-color] duration-300 ease-out",
        glow === "primary" && "shadow-[var(--glow-primary)]",
        glow === "cyan" && "shadow-[var(--glow-cyan)]",
        className,
      )}
      {...rest}
    >
      {tilt && !reduced && (
        <span
          aria-hidden
          className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover/card:opacity-100"
          style={{
            background:
              "radial-gradient(420px circle at var(--mx, 50%) var(--my, 0%), oklch(1 0 0 / 8%), transparent 60%)",
          }}
        />
      )}
      <div className="relative">{children}</div>
    </div>
  );
}