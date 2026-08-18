export const suggestedQuestions = [
  "Why did Arjun score higher than Rohan?",
  "What is the single biggest weakness in this report?",
  "How confident are you in this evaluation?",
  "Build a 6-week training plan for Arjun",
  "Was this score influenced by the player's background?",
];

interface GeminiCannedAnswer {
  match: RegExp;
  answer: string;
}

export const geminiAnswers: GeminiCannedAnswer[] = [
  {
    match: /higher|compare|vs|better|than/i,
    answer:
      "Arjun Mehra scores 87 against Rohan Desai's 81, and the gap comes from three specific places rather than an overall impression.\n\nFirst, consistency: Arjun's innings-to-innings standard deviation is 6.2 versus Rohan's 13.4, so his output is far more predictable across sessions. Second, shot selection: his false-shot rate against spin is 9%, while Rohan's length discipline slips to 28% short-of-good-length in death overs. Third, defensive technique: Arjun keeps his head over the front knee on 9 of 10 deliveries.\n\nOne caveat worth stating plainly — they are evaluated in different disciplines. Rohan's bowling score of 90 is the strongest single metric across both players. If you are selecting a specialist seamer, the overall number is the wrong comparison to use.",
  },
  {
    match: /weak|improve|worst|problem/i,
    answer:
      "The clearest weakness is back-foot defence against deliveries above 130 km/h, logged at 02:31 in the footage with 89% confidence.\n\nAcross four short balls the back foot stayed planted, which forced a cramped push away from the body. This single pattern accounts for −8 points of the batting score. The encouraging part: by 13:58 he played two controlled pulls along the ground, which suggests the fault is correctable within a session rather than structural.\n\nI would prioritise short-ball throwdowns from 16 yards before anything else in the plan.",
  },
  {
    match: /confiden|trust|sure|reliab/i,
    answer:
      "Overall confidence is 94%, and I want to be precise about what that does and does not mean.\n\nHigh-confidence components: batting technique (96% on the cover drive evidence) and fielding (92%), both measured from clean, unoccluded frames. Lower-confidence components: fitness inference at 76% — it is derived from movement cadence, not real physiological data — and the late-innings shot-selection entry at 68%, which rests on only two deliveries.\n\nThe bowling dimension is the weakest link: 6 deliveries is not enough to score fairly, so I down-weighted it rather than guessing. Treat the bowling figure as provisional.",
  },
  {
    match: /train|plan|drill|develop|week/i,
    answer:
      "Here is a six-week plan targeting the two faults with the largest measured score impact.\n\nWeeks 1–2 — Back-foot defence: short-ball throwdowns from 16 yards, 4 sets of 15, with the sighting cue on the bowler's wrist. Expected gain +5 to +7 Batting.\n\nWeeks 3–4 — Strike rotation: constrained net games where only singles count for the first 24 balls. This targets the overs 11–14 dot cluster directly. Expected gain +4 Consistency.\n\nWeeks 5–6 — Sample expansion and retest: 6 overs of off-break per match simulation to raise evaluation confidence from 94% to roughly 97%, then a fresh video analysis to verify the gains held under match conditions.\n\nI would not add a fourth workstream. Two faults, corrected properly, will move the overall score more than four partially addressed ones.",
  },
  {
    match: /fair|bias|background|region|caste|reput|popular/i,
    answer:
      "No. The player's background had no path into this score, and I can be specific about why.\n\nName, region, school, team affiliation, family and economic indicators are stripped before the feature vector reaches the model — 0 of the 47 active features encode a protected attribute. The evaluation uses only technique (pose-derived biomechanics), performance (tracked on-field events), consistency (variance across sessions) and accuracy (execution against intent).\n\nWe also monitor outcomes, not just inputs: regional score variance across the current cohort is ±1.8 points, which is within expected sampling noise. If that number drifted, the model card would be flagged for review before selection decisions were made.",
  },
  {
    match: /select|shortlist|pick|recommend/i,
    answer:
      "Based on the current cohort I would shortlist Arjun Mehra and Kavya Nair with confidence, and Rohan Desai conditionally.\n\nArjun (87, 94% confidence) offers the most reliable evidence base. Kavya (84, 91%) has elite fielding at 0.9s pickup-to-release and genuine all-round value, though only 19 matches have been analysed. Rohan (81, 88%) has the single best specialist skill in the group — a bowling score of 90 — but his consistency at 76 needs a human read on temperament that I cannot measure from footage.\n\nThis is a recommendation, not a decision. I would treat the Rohan call as one for the selection panel rather than the model.",
  },
];

export const geminiFallback =
  "I can answer that from the current report, though let me be clear about my limits.\n\nThis evaluation is built from 18:42 of footage covering 118 tracked events across five metric families: batting 91, bowling 62, fielding 84, fitness 88 and consistency 89. Where the evidence is thin — the 6-delivery bowling sample in particular — I down-weight rather than extrapolate.\n\nIf you ask about a specific metric, timestamp or comparison, I can point you to the exact evidence entry that drove it.";

export const comparisonVerdict =
  "Arjun Mehra is the stronger overall prospect at this stage, but the margin is narrower than the six-point gap suggests.\n\nHis advantage is reliability rather than peak ability: a consistency score of 89 against Rohan's 76, and a false-shot rate of 9% against spin. Where Rohan wins decisively is specialist bowling — 90 against 62 — with seam position retained upright on 88% of deliveries.\n\nMy recommendation: if the squad needs a dependable top-order batter, take Arjun. If the gap in the squad is a front-line seamer, the overall score is misleading and Rohan is the correct pick. I hold this at 86% confidence; Rohan's smaller consistency sample is the main source of uncertainty.";

export const fairnessNarrative =
  "This score was produced from what the player did on the field, and nothing else.\n\nBefore any evaluation begins, identifying details — name, region, school, team, family and economic background — are removed from the data the model sees. What remains is movement, timing, execution and outcome: how the feet move, where the head sits at contact, how repeatable the action is, and whether performance holds under pressure.\n\nWe also check the result, not just the process. Across the current cohort, average scores vary by only ±1.8 points between regions, which is what you would expect from sampling noise rather than bias. Eleven human overrides were accepted in the last 30 days, and each one feeds back into the model card review.\n\nThe system is designed to assist selectors, not to replace their judgement. Where evidence is thin, it says so.";