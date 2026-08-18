import { cn } from "@/lib/utils";

/** Renders streamed Gemini text with paragraph breaks and a live caret. */
export function StreamBlock({
  text,
  streaming,
  className,
}: {
  text: string;
  streaming?: boolean;
  className?: string;
}) {
  const paragraphs = text.split("\n\n");
  return (
    <div className={cn("space-y-3 text-sm leading-relaxed text-foreground/90", className)}>
      {paragraphs.map((p, i) => (
        <p key={i}>
          {p}
          {streaming && i === paragraphs.length - 1 && (
            <span className="animate-caret ml-0.5 inline-block h-4 w-[2px] translate-y-0.5 bg-cyan" />
          )}
        </p>
      ))}
    </div>
  );
}