import { comparisonVerdict, fairnessNarrative, geminiAnswers, geminiFallback } from "@/data/gemini";

/**
 * Placeholder Gemini service.
 *
 * Every Gemini-authored surface in the app calls through here. To connect a real
 * backend later (Spring Boot -> Python AI service -> Gemini API), replace the
 * bodies of these functions with fetch calls that return the same shapes. No
 * component needs to change.
 */

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export function resolveAnswer(question: string): string {
  const hit = geminiAnswers.find((a) => a.match.test(question));
  return hit ? hit.answer : geminiFallback;
}

export interface StreamOptions {
  /** average ms between emitted words */
  speed?: number;
  signal?: AbortSignal;
}

/** Streams text word-by-word, emulating a live model response. */
export async function streamText(
  text: string,
  onToken: (partial: string) => void,
  { speed = 26, signal }: StreamOptions = {},
): Promise<string> {
  const tokens = text.split(/(\s+)/);
  let acc = "";
  for (const token of tokens) {
    if (signal?.aborted) break;
    acc += token;
    onToken(acc);
    if (token.trim().length > 0) {
      await wait(speed + (token.length > 8 ? 22 : 0));
    }
  }
  return acc;
}

export async function askGemini(
  question: string,
  onToken: (partial: string) => void,
  options?: StreamOptions,
): Promise<string> {
  await wait(320);
  return streamText(resolveAnswer(question), onToken, options);
}

export async function streamComparisonVerdict(
  onToken: (partial: string) => void,
  options?: StreamOptions,
) {
  return streamText(comparisonVerdict, onToken, options);
}

export async function streamFairnessNarrative(
  onToken: (partial: string) => void,
  options?: StreamOptions,
) {
  return streamText(fairnessNarrative, onToken, options);
}

export { comparisonVerdict, fairnessNarrative };