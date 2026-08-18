import { useState } from "react";
import { CheckCircle2, RefreshCw, ThumbsDown, ThumbsUp } from "lucide-react";
import { toast } from "sonner";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { useApp } from "@/context/AppContext";
import type { FeedbackVerdict } from "@/types";
import { cn } from "@/lib/utils";

const options: {
  verdict: FeedbackVerdict;
  label: string;
  icon: typeof ThumbsUp;
  tone: string;
}[] = [
  { verdict: "agree", label: "Agree with AI", icon: ThumbsUp, tone: "text-success" },
  { verdict: "disagree", label: "Disagree with AI", icon: ThumbsDown, tone: "text-danger" },
  { verdict: "review", label: "Request review", icon: RefreshCw, tone: "text-warning" },
];

const verdictCopy: Record<FeedbackVerdict, string> = {
  agree: "Agreed with AI",
  disagree: "Disagreed with AI",
  review: "Review requested",
};

interface Props {
  /** Where the feedback was captured, shown in the confirmation line. */
  context?: string;
  className?: string;
}

export function AgreeDisagreeReview({ context = "this evaluation", className }: Props) {
  const { activePlayerId, feedback, submitFeedback } = useApp();
  const [draft, setDraft] = useState<FeedbackVerdict | null>(null);
  const [reason, setReason] = useState("");
  const [comments, setComments] = useState("");
  const [priority, setPriority] = useState<"normal" | "high">("normal");
  const [error, setError] = useState(false);

  const submitted = feedback !== null;

  const commit = (verdict: FeedbackVerdict, extra?: { reason: string }) => {
    submitFeedback({
      verdict,
      reason: extra?.reason ?? "Evaluation matches human read.",
      ...(comments.trim() ? { comments: comments.trim() } : {}),
      ...(verdict === "review" ? { priority } : {}),
      submittedAt: new Date().toISOString(),
      playerId: activePlayerId,
    });
    toast.success("Feedback logged", {
      description: `${verdictCopy[verdict]} — recorded against the model card.`,
    });
    setDraft(null);
  };

  const select = (verdict: FeedbackVerdict) => {
    if (submitted) return;
    if (verdict === "agree") return commit("agree");
    setError(false);
    setDraft((d) => (d === verdict ? null : verdict));
  };

  const submitForm = () => {
    if (!draft) return;
    if (!reason.trim()) {
      setError(true);
      return;
    }
    commit(draft, { reason: reason.trim() });
  };

  return (
    <GlassCard className={cn("p-6", className)}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="telemetry text-lg text-foreground">Human in the loop</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            The model assists the decision — your verdict on {context} is logged alongside it.
          </p>
        </div>
        {submitted && (
          <span className="flex items-center gap-1.5 rounded-full bg-success/15 px-3 py-1 text-[11px] text-success">
            <CheckCircle2 className="h-3.5 w-3.5" /> Feedback submitted
          </span>
        )}
      </div>

      {submitted && feedback ? (
        <div className="mt-5 rounded-xl border border-border bg-surface/50 p-4">
          <p className="text-sm text-foreground">{verdictCopy[feedback.verdict]}</p>
          <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">{feedback.reason}</p>
          {feedback.comments && (
            <p className="mt-1.5 text-xs leading-relaxed text-muted-foreground">
              Notes: {feedback.comments}
            </p>
          )}
          <p className="mt-3 text-[11px] text-muted-foreground">
            Logged {new Date(feedback.submittedAt).toLocaleString()}
            {feedback.priority ? ` · ${feedback.priority} priority` : ""}
          </p>
        </div>
      ) : (
        <>
          <div className="mt-5 grid gap-3 sm:grid-cols-3">
            {options.map((o) => (
              <button
                key={o.verdict}
                type="button"
                onClick={() => select(o.verdict)}
                aria-pressed={draft === o.verdict}
                className={cn(
                  "focus-ring lift flex items-center gap-2.5 rounded-xl border bg-surface/50 p-4 text-left text-sm text-foreground transition-colors",
                  draft === o.verdict ? "border-primary/60" : "border-border hover:border-primary/40",
                )}
              >
                <o.icon className={cn("h-4 w-4 shrink-0", o.tone)} />
                {o.label}
              </button>
            ))}
          </div>

          {draft && (
            <div className="animate-fade-up mt-4 rounded-xl border border-border bg-surface/40 p-4">
              <label className="label-tech block" htmlFor="hitl-reason">
                Reason <span className="text-danger">*</span>
              </label>
              <textarea
                id="hitl-reason"
                value={reason}
                onChange={(e) => {
                  setReason(e.target.value);
                  if (e.target.value.trim()) setError(false);
                }}
                rows={2}
                placeholder={
                  draft === "disagree"
                    ? "What did the model get wrong?"
                    : "What should a second reviewer look at?"
                }
                className="focus-ring mt-2 w-full rounded-lg border border-border bg-surface/60 p-3 text-sm text-foreground placeholder:text-muted-foreground"
              />
              {error && <p className="mt-1.5 text-[11px] text-danger">A reason is required.</p>}

              <label className="label-tech mt-4 block" htmlFor="hitl-comments">
                Additional comments (optional)
              </label>
              <textarea
                id="hitl-comments"
                value={comments}
                onChange={(e) => setComments(e.target.value)}
                rows={2}
                placeholder="Context, match conditions, prior observations…"
                className="focus-ring mt-2 w-full rounded-lg border border-border bg-surface/60 p-3 text-sm text-foreground placeholder:text-muted-foreground"
              />

              {draft === "review" && (
                <div className="mt-4">
                  <span className="label-tech block">Priority</span>
                  <div className="mt-2 flex gap-2">
                    {(["normal", "high"] as const).map((p) => (
                      <button
                        key={p}
                        type="button"
                        onClick={() => setPriority(p)}
                        aria-pressed={priority === p}
                        className={cn(
                          "focus-ring rounded-lg border px-3.5 py-1.5 text-xs capitalize transition-colors",
                          priority === p
                            ? "border-primary/60 bg-primary/15 text-foreground"
                            : "border-border bg-surface/60 text-muted-foreground hover:text-foreground",
                        )}
                      >
                        {p}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="mt-5 flex flex-wrap gap-2">
                <MagneticButton onClick={submitForm}>Submit feedback</MagneticButton>
                <MagneticButton variant="ghost" onClick={() => setDraft(null)}>
                  Cancel
                </MagneticButton>
              </div>
            </div>
          )}
        </>
      )}
    </GlassCard>
  );
}
