import { cn } from "@/lib/utils";
import { useRef, type ButtonHTMLAttributes, type ReactNode } from "react";
import { useReducedMotion } from "@/hooks/useReducedMotion";

interface MagneticButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  variant?: "primary" | "ghost" | "outline";
  size?: "md" | "lg";
}

const variants = {
  primary:
    "bg-primary text-primary-foreground hover:brightness-110 shadow-[var(--glow-primary)] font-semibold",
  outline: "border border-border bg-surface/60 text-foreground hover:border-primary/50",
  ghost: "text-muted-foreground hover:text-foreground hover:bg-surface-2/60",
};

export function MagneticButton({
  children,
  className,
  variant = "primary",
  size = "md",
  ...rest
}: MagneticButtonProps) {
  const ref = useRef<HTMLButtonElement>(null);
  const reduced = useReducedMotion();

  const onMove = (e: React.PointerEvent<HTMLButtonElement>) => {
    const el = ref.current;
    if (!el || reduced) return;
    const r = el.getBoundingClientRect();
    const dx = e.clientX - (r.left + r.width / 2);
    const dy = e.clientY - (r.top + r.height / 2);
    el.style.transform = `translate(${dx * 0.16}px, ${dy * 0.22}px)`;
  };

  const reset = () => {
    if (ref.current) ref.current.style.transform = "";
  };

  return (
    <button
      ref={ref}
      onPointerMove={onMove}
      onPointerLeave={reset}
      className={cn(
        "focus-ring inline-flex items-center justify-center gap-2 rounded-xl transition-[transform,background-color,box-shadow,filter] duration-200 disabled:pointer-events-none disabled:opacity-50",
        size === "lg" ? "px-7 py-3.5 text-base" : "px-5 py-2.5 text-sm",
        variants[variant],
        className,
      )}
      {...rest}
    >
      {children}
    </button>
  );
}