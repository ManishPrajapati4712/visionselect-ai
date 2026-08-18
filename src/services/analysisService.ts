import { analysisStages, fairnessAudit, fairnessFactors } from "@/data/analysis";
import { evidenceTimeline, metricContributions } from "@/data/evidence";

/** Placeholder analysis service — replace with the Python AI service later. */

export const getStages = () => analysisStages;
export const getEvidence = () => evidenceTimeline;
export const getContributions = () => metricContributions;
export const getFairnessFactors = () => fairnessFactors;
export const getFairnessAudit = () => fairnessAudit;

export const FAIRNESS_SCORE = 96;

export function simulateUpload(onProgress: (pct: number) => void, onDone: () => void) {
  let pct = 0;
  const id = window.setInterval(() => {
    pct = Math.min(100, pct + Math.random() * 13 + 5);
    onProgress(Math.round(pct));
    if (pct >= 100) {
      window.clearInterval(id);
      window.setTimeout(onDone, 450);
    }
  }, 220);
  return () => window.clearInterval(id);
}