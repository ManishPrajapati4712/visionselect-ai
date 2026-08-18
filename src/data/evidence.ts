import type { EvidenceEntry, MetricContribution } from "@/types";

export const evidenceTimeline: EvidenceEntry[] = [
  {
    id: "ev-1",
    timestamp: "00:47",
    seconds: 47,
    title: "Balanced trigger movement detected",
    detail:
      "Head remained over front knee through the delivery stride on 9 of 10 balls. Model flagged this as a repeatable, coachable base.",
    metric: "batting",
    impact: 6,
    confidence: 93,
  },
  {
    id: "ev-2",
    timestamp: "02:31",
    seconds: 151,
    title: "Poor footwork against back-of-length",
    detail:
      "Back foot stayed planted across 4 short deliveries above 130 km/h, forcing a cramped defensive push away from the body.",
    metric: "batting",
    impact: -8,
    confidence: 89,
  },
  {
    id: "ev-3",
    timestamp: "04:12",
    seconds: 252,
    title: "Excellent cover drive execution",
    detail:
      "Front elbow high, contact under the eyeline, weight transferred fully. Ball tracked at 118 km/h off the bat through extra cover.",
    metric: "batting",
    impact: 12,
    confidence: 96,
  },
  {
    id: "ev-4",
    timestamp: "06:05",
    seconds: 365,
    title: "Dot-ball cluster in overs 11–14",
    detail:
      "7 consecutive dots. Strike rotation opportunities to deep point were declined twice — a decision pattern, not a technique fault.",
    metric: "consistency",
    impact: -5,
    confidence: 81,
  },
  {
    id: "ev-5",
    timestamp: "08:39",
    seconds: 519,
    title: "Clean boundary save at deep midwicket",
    detail:
      "Sprint speed peaked at 7.4 m/s with a controlled slide and accurate one-bounce return to the keeper.",
    metric: "fielding",
    impact: 9,
    confidence: 92,
  },
  {
    id: "ev-6",
    timestamp: "11:22",
    seconds: 682,
    title: "Recovery pace held between overs",
    detail:
      "Heart-rate proxy from movement cadence showed no measurable drop-off in the final third of the session.",
    metric: "fitness",
    impact: 7,
    confidence: 76,
  },
  {
    id: "ev-7",
    timestamp: "13:58",
    seconds: 838,
    title: "Repeatable pull shot under bounce",
    detail:
      "Two controlled pulls kept along the ground. Adjustment made after the 02:31 weakness — evidence of in-session learning.",
    metric: "batting",
    impact: 8,
    confidence: 88,
  },
  {
    id: "ev-8",
    timestamp: "16:40",
    seconds: 1000,
    title: "Late-innings shot selection dip",
    detail:
      "Two attacking shots played to good-length balls outside off. Low sample — model reports reduced confidence here.",
    metric: "consistency",
    impact: -4,
    confidence: 68,
  },
];

export const metricContributions: MetricContribution[] = [
  {
    metric: "batting",
    label: "Batting Technique",
    weight: 0.32,
    contribution: 18,
    note: "Driven by drive execution and contact quality across 34 scoring shots.",
  },
  {
    metric: "consistency",
    label: "Consistency",
    weight: 0.24,
    contribution: 11,
    note: "Low innings-to-innings variance, slightly reduced by the overs 11–14 dot cluster.",
  },
  {
    metric: "fielding",
    label: "Fielding",
    weight: 0.18,
    contribution: 9,
    note: "Ground coverage and throw accuracy above the U19 benchmark.",
  },
  {
    metric: "fitness",
    label: "Fitness & Endurance",
    weight: 0.16,
    contribution: 6,
    note: "No measurable output decay in the final third of the session.",
  },
  {
    metric: "bowling",
    label: "Bowling",
    weight: 0.1,
    contribution: -3,
    note: "Only 6 deliveries bowled — contribution intentionally down-weighted for low sample.",
  },
];