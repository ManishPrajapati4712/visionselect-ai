import { useEffect, useRef, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { CornerDownLeft, MessageSquare } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { StreamBlock } from "@/components/common/StreamBlock";
import { MagneticButton } from "@/components/common/MagneticButton";
import { askGemini } from "@/services/geminiService";
import { suggestedQuestions } from "@/data/gemini";
import { useApp } from "@/context/AppContext";
import { getPlayer } from "@/data/players";
import type { ChatMessage } from "@/types";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/assistant")({
  head: () => ({
    meta: [
      { title: "Gemini assistant — VisionSelect AI" },
      {
        name: "description",
        content:
          "Question any evaluation in plain language. Gemini answers with evidence, comparisons, training plans and honest confidence levels.",
      },
      { property: "og:title", content: "Gemini assistant — VisionSelect AI" },
      {
        property: "og:description",
        content: "A sports analyst you can interrogate, not just read.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Assistant,
});

let seq = 0;
const nextId = () => `m-${++seq}`;

function Assistant() {
  const { activePlayerId } = useApp();
  const player = getPlayer(activePlayerId);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages]);

  const send = async (question: string) => {
    const q = question.trim();
    if (!q || busy) return;
    setInput("");
    setBusy(true);
    const answerId = nextId();
    setMessages((m) => [
      ...m,
      { id: nextId(), role: "user", content: q },
      { id: answerId, role: "assistant", content: "", streaming: true },
    ]);
    await askGemini(q, (partial) =>
      setMessages((m) => m.map((x) => (x.id === answerId ? { ...x, content: partial } : x))),
    );
    setMessages((m) => m.map((x) => (x.id === answerId ? { ...x, streaming: false } : x)));
    setBusy(false);
    inputRef.current?.focus();
  };

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Conversational intelligence"
        title="Gemini assistant"
        description={`Ask anything about ${player.name}'s evaluation — comparisons, weaknesses, training plans or how confident the model actually is.`}
      />

      <div className="grid gap-6 xl:grid-cols-[1fr_320px]">
        <GlassCard glow="primary" className="flex min-h-[62vh] flex-col p-0">
          <div className="flex items-center gap-3 border-b border-border px-6 py-4">
            <span className="grid h-9 w-9 place-items-center rounded-lg bg-primary/20">
              <MessageSquare className="h-4 w-4 text-cyan" />
            </span>
            <div>
              <p className="text-sm text-foreground">Gemini sports analyst</p>
              <p className="text-[11px] text-muted-foreground">
                Context: {player.name} · score {player.overallScore} · {player.confidence}% confidence
              </p>
            </div>
            <span className="ml-auto inline-flex items-center gap-2 rounded-full border border-border bg-surface/60 px-3 py-1.5 text-[11px] text-muted-foreground">
              <span className="h-1.5 w-1.5 rounded-full bg-success" />
              {busy ? "Thinking…" : "Online"}
            </span>
          </div>

          <div className="flex-1 space-y-5 overflow-y-auto p-6">
            {messages.length === 0 && (
              <div className="animate-fade-up grid h-full place-items-center text-center">
                <div className="max-w-md">
                  <p className="telemetry text-lg text-foreground">
                    Interrogate the evaluation
                  </p>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                    Every answer is grounded in the 118 tracked events from this session. Where the
                    evidence is thin, Gemini will say so rather than guess.
                  </p>
                  <div className="mt-6 flex flex-wrap justify-center gap-2">
                    {suggestedQuestions.slice(0, 3).map((q) => (
                      <button
                        key={q}
                        onClick={() => void send(q)}
                        className="focus-ring rounded-full border border-border bg-surface/50 px-3 py-2 text-xs text-muted-foreground transition-colors hover:border-primary/40 hover:text-foreground"
                      >
                        {q}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {messages.map((m) =>
              m.role === "user" ? (
                <div key={m.id} className="flex justify-end">
                  <p className="max-w-[80%] rounded-2xl rounded-br-md bg-primary px-4 py-2.5 text-sm text-primary-foreground">
                    {m.content}
                  </p>
                </div>
              ) : (
                <div key={m.id} className="animate-fade-up flex gap-3">
                  <span className="telemetry mt-0.5 grid h-7 w-7 shrink-0 place-items-center rounded-md bg-primary/20 text-[10px] text-cyan">
                    AI
                  </span>
                  <div className="max-w-[85%]">
                    <StreamBlock text={m.content} streaming={m.streaming ?? false} />
                  </div>
                </div>
              ),
            )}
            <div ref={endRef} />
          </div>

          <div className="border-t border-border p-4">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                void send(input);
              }}
              className="flex items-end gap-3 rounded-xl border border-border bg-surface/60 p-2.5"
            >
              <textarea
                ref={inputRef}
                rows={1}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    void send(input);
                  }
                }}
                placeholder="Ask about this evaluation…"
                className="max-h-32 flex-1 resize-none bg-transparent px-2 py-1.5 text-sm text-foreground outline-none placeholder:text-muted-foreground"
              />
              <MagneticButton type="submit" disabled={busy || !input.trim()}>
                Send <CornerDownLeft className="h-3.5 w-3.5" />
              </MagneticButton>
            </form>
          </div>
        </GlassCard>

        <div className="space-y-6">
          <GlassCard className="p-6">
            <h2 className="telemetry text-lg text-foreground">Suggested questions</h2>
            <div className="mt-4 space-y-2">
              {suggestedQuestions.map((q) => (
                <button
                  key={q}
                  onClick={() => void send(q)}
                  disabled={busy}
                  className={cn(
                    "focus-ring w-full rounded-xl border border-border bg-surface/40 px-4 py-3 text-left text-sm text-muted-foreground transition-colors",
                    "hover:border-primary/35 hover:text-foreground disabled:opacity-50",
                  )}
                >
                  {q}
                </button>
              ))}
            </div>
          </GlassCard>

          <GlassCard className="p-6">
            <p className="label-tech">How answers are grounded</p>
            <ul className="mt-3 space-y-2.5 text-xs leading-relaxed text-muted-foreground">
              <li>· Answers cite the evidence timeline, never opinion.</li>
              <li>· Confidence is reported per-dimension, not just overall.</li>
              <li>· Low-sample metrics are down-weighted and flagged.</li>
              <li>· Background and reputation features are never available to the model.</li>
            </ul>
          </GlassCard>
        </div>
      </div>
    </AppLayout>
  );
}
