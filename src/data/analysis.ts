import type { AnalysisRecord, AnalysisStage, FairnessFactor, TrainingItem } from "@/types";

export const analysisStages: AnalysisStage[] = [
  {
    id: "video",
    label: "Video Processing",
    description: "Decoding 18:42 of footage · stabilising frames · 24 fps sampling",
    durationMs: 3200,
    geminiSnippet:
      "Footage quality is good: consistent side-on framing at 24 fps with only two brief occlusions. I can evaluate technique reliably from this angle.",
  },
  {
    id: "detection",
    label: "Player Detection",
    description: "Isolating subject · pose keypoints · bat and crease tracking",
    durationMs: 3600,
    geminiSnippet:
      "Subject isolated across 96% of frames. Pose keypoints are stable through the delivery stride, so footwork and head position are measurable rather than inferred.",
  },
  {
    id: "performance",
    label: "Performance Analysis",
    description: "Scoring 5 metric families across 118 tracked events",
    durationMs: 4200,
    geminiSnippet:
      "Batting technique is the strongest signal — 82% clean contact on front-foot drives. The weakest cluster is back-foot defence against deliveries above 130 km/h.",
  },
  {
    id: "gemini",
    label: "Gemini Intelligence",
    description: "Reasoning over evidence · cross-checking bias controls",
    durationMs: 4600,
    geminiSnippet:
      "Cross-checking the score against the fairness controls: no reputation, team, or background features were used. The evaluation rests entirely on technique, output, and consistency signals.",
  },
  {
    id: "report",
    label: "Final Report",
    description: "Composing evidence timeline · confidence calibration",
    durationMs: 2600,
    geminiSnippet:
      "Final score 87 with 94% confidence. Confidence is high but not absolute — the bowling sample was only 6 deliveries, so I have down-weighted that dimension.",
  },
];

export const recentAnalyses: AnalysisRecord[] = [
  {
    id: "an-1041",
    playerId: "arjun-mehra",
    playerName: "Arjun Mehra",
    venue: "Pune Regional Ground",
    date: "Aug 1, 2026",
    score: 87,
    confidence: 94,
    status: "complete",
    duration: "18:42",
  },
  {
    id: "an-1040",
    playerId: "kavya-nair",
    playerName: "Kavya Nair",
    venue: "Kochi Academy Nets",
    date: "Jul 29, 2026",
    score: 84,
    confidence: 91,
    status: "complete",
    duration: "22:10",
  },
  {
    id: "an-1039",
    playerId: "rohan-desai",
    playerName: "Rohan Desai",
    venue: "Surat District Stadium",
    date: "Jul 28, 2026",
    score: 81,
    confidence: 88,
    status: "review",
    duration: "15:03",
  },
  {
    id: "an-1038",
    playerId: "imran-shaikh",
    playerName: "Imran Shaikh",
    venue: "Deccan Blues Centre",
    date: "Jul 24, 2026",
    score: 78,
    confidence: 83,
    status: "complete",
    duration: "19:55",
  },
];

export const activityFeed = [
  {
    id: "a1",
    text: "Gemini flagged a technique regression for Imran Shaikh in overs 14–18.",
    time: "12m ago",
    tone: "warning" as const,
  },
  {
    id: "a2",
    text: "Selector panel accepted the AI ranking for the Western Zone shortlist.",
    time: "1h ago",
    tone: "success" as const,
  },
  {
    id: "a3",
    text: "Fairness audit completed — 0 protected attributes present in the feature set.",
    time: "3h ago",
    tone: "info" as const,
  },
  {
    id: "a4",
    text: "Coach Verma requested a human review on Rohan Desai's bowling score.",
    time: "6h ago",
    tone: "info" as const,
  },
];

export const trainingPlan: TrainingItem[] = [
  {
    id: "t1",
    focus: "Back-foot defence vs pace",
    drill: "Short-ball throwdowns from 16 yards, 4 sets of 15, sighting cue on the bowler's wrist",
    frequency: "3× per week",
    expectedGain: "+5 to +7 Batting over 6 weeks",
    priority: "high",
  },
  {
    id: "t2",
    focus: "Strike rotation in middle overs",
    drill: "Constrained net game — only singles count for the first 24 balls",
    frequency: "2× per week",
    expectedGain: "+4 Consistency",
    priority: "high",
  },
  {
    id: "t3",
    focus: "Bowling sample expansion",
    drill: "6 overs of off-break in match simulation to lift evaluation confidence",
    frequency: "Weekly",
    expectedGain: "Confidence 94% → 97%",
    priority: "medium",
  },
  {
    id: "t4",
    focus: "Fielding transition speed",
    drill: "Pickup-to-release ladder, target under 1.0s",
    frequency: "2× per week",
    expectedGain: "+3 Fielding",
    priority: "low",
  },
];

export const fairnessFactors: FairnessFactor[] = [
  {
    label: "Technique",
    description: "Pose-derived biomechanics: footwork, head position, seam and wrist control.",
    included: true,
  },
  {
    label: "Performance",
    description: "Measured on-field outcomes tracked frame by frame from the footage.",
    included: true,
  },
  {
    label: "Consistency",
    description: "Variance across innings, sessions and pressure phases.",
    included: true,
  },
  {
    label: "Accuracy",
    description: "Execution precision against the intended line, length or shot.",
    included: true,
  },
  {
    label: "Popularity",
    description: "Social following, media mentions and public profile are never ingested.",
    included: false,
  },
  {
    label: "Reputation",
    description: "Prior selection history and franchise affiliation are excluded from features.",
    included: false,
  },
  {
    label: "Background",
    description: "Region, school, family, economic status and name are removed before scoring.",
    included: false,
  },
];

export const fairnessAudit = [
  { label: "Protected attributes in feature set", value: "0 of 47", tone: "success" as const },
  { label: "Regional score variance", value: "±1.8 pts", tone: "success" as const },
  { label: "Model card version", value: "vs-cricket-3.2", tone: "info" as const },
  { label: "Human overrides last 30 days", value: "11 accepted", tone: "info" as const },
];