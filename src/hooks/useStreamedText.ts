import { useCallback, useEffect, useRef, useState } from "react";
import { streamText } from "@/services/geminiService";
import { useReducedMotion } from "./useReducedMotion";

/** Runs a mock Gemini stream and exposes the growing text. */
export function useStreamedText(source: string, autoStart = true, speed = 24) {
  const reduced = useReducedMotion();
  const [text, setText] = useState("");
  const [done, setDone] = useState(false);
  const controller = useRef<AbortController | null>(null);

  const run = useCallback(() => {
    controller.current?.abort();
    const ac = new AbortController();
    controller.current = ac;
    setDone(false);
    if (reduced) {
      setText(source);
      setDone(true);
      return;
    }
    setText("");
    void streamText(source, setText, { speed, signal: ac.signal }).then(() => {
      if (!ac.signal.aborted) setDone(true);
    });
  }, [source, speed, reduced]);

  useEffect(() => {
    if (autoStart) run();
    return () => controller.current?.abort();
  }, [autoStart, run]);

  return { text, done, restart: run };
}